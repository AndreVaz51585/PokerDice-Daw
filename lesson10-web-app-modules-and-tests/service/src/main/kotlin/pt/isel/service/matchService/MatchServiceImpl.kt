import kotlinx.coroutines.runBlocking
import org.springframework.stereotype.Service
import pt.isel.domain.Game.Face
import pt.isel.domain.Game.Hand
import pt.isel.domain.Game.Hand.Companion.getCombination
import pt.isel.domain.Game.Match.Match
import pt.isel.domain.Game.Match.MatchEvent
import pt.isel.domain.Game.Match.MatchPlayer
import pt.isel.domain.Game.Match.MatchState
import pt.isel.domain.Game.money.BankedMatch
import pt.isel.domain.Game.money.BankedMatchEngine
import pt.isel.domain.Game.money.Wallet
import pt.isel.domain.Game.pokerDice.Command
import pt.isel.domain.Game.pokerDice.GamePhase
import pt.isel.domain.Game.pokerDice.createNewGame
import pt.isel.repo.RepositoryLobby
import pt.isel.repo.RepositoryMatch
import pt.isel.repo.TransactionManager
import pt.isel.service.Auxiliary.*
import pt.isel.service.match.BankedGameMatchEngine
import pt.isel.service.matchService.MatchEventPublisher
import pt.isel.service.matchService.MatchManager
import pt.isel.service.matchService.MatchService
import pt.isel.service.matchService.MatchServiceError
import pt.isel.service.walletService.WalletService
import pt.isel.domain.Game.Match.MatchEvent.RoundSummary.RoundHandView
import pt.isel.domain.Game.Match.MatchEvent.RoundSummary.DiceView
import pt.isel.domain.Game.Match.MatchEvent.RoundSummary

import pt.isel.domain.Game.pokerDice.PlayerState

@Service
class MatchServiceImpl(
    private val repoLobby: RepositoryLobby,
    private val repoMatch: RepositoryMatch,
    private val walletService: WalletService,
    private val trxManager: TransactionManager,
    private val matchManager: MatchManager,
    private val eventPublisher: MatchEventPublisher
) : MatchService {


    override fun applyCommand(matchId: Int, cmd: Command): Either<MatchServiceError, BankedMatch> {
        val engine = matchManager.get(matchId) ?: return failure(MatchServiceError.MatchNotFound)


        val prevState = engine.snapshot()


        val res = runBlocking { engine.dispatch(cmd) }
        if (res.isFailure) {
            return failure(MatchServiceError.Unknown)
        }


        val afterState = engine.snapshot()


        publishEventsBasedOnState(matchId, prevState, afterState, cmd)

        return success(afterState)
    }


    private fun buildRoundHandsView(
        roundHands: Map<Int, Hand>
    ): Map<Int, RoundHandView> {

        return roundHands.mapValues { (_, hand) ->
            val (combination, _) = hand.getCombination()
            RoundHandView(
                dices = hand.faces.map { DiceView(it) },
                combination = combination.toString()
            )
        }
    }





    private fun publishEventsBasedOnState(
        matchId: Int,
        prevState: BankedMatch,
        afterState: BankedMatch,
        cmd: Command
    ) {
        val roundEnded = prevState.game.rounds.size != afterState.game.rounds.size
        val gamePhase = afterState.game.phase
        val lastRound = afterState.game.rounds.last() //

        if (gamePhase == GamePhase.FINISHED) {

            val completedRound = prevState.game.rounds.last()

            val handsSnapshot: Map<Int, Hand> =
                completedRound.hands.mapValues { (_, hand) ->
                    Hand(hand.faces.toList())
                }

            eventPublisher.publish(
                matchId,
                MatchEvent.RoundSummary(
                    roundNumber = completedRound.number,
                    winners = completedRound.winners ?: emptyList(),
                    prize = completedRound.pot,
                    wallets = afterState.wallets,
                    playersAndCombinations = buildRoundHandsView(handsSnapshot)
                )
            )

            val match = repoMatch.findById(matchId) ?: return
            repoMatch.save(
                match.copy(
                    state = MatchState.FINISHED,
                    finishedAt = java.time.Instant.now()
                )
            )

            repoLobby.listPlayers(match.lobbyId).forEach { player ->
                repoLobby.remove(match.lobbyId, player.id)
                afterState.wallets[player.id]?.let {
                    walletService.update(
                        Wallet(player.id, it.currentBalance)
                    )
                }
            }

            val winner =
                afterState.game.rounds
                    .flatMap { it.winners ?: emptyList() }
                    .groupingBy { it }
                    .eachCount()
                    .maxByOrNull { it.value }!!.key

            eventPublisher.publish(
                matchId,
                MatchEvent.GameEndPayload(
                    winner = winner,
                    wallets = afterState.wallets
                )
            )
        }

        if (roundEnded) {

            val completedRound = prevState.game.rounds.last()

            // Snapshot REAL da ronda
            val handsSnapshot: Map<Int, Hand> =
                completedRound.hands.mapValues { (_, hand) ->
                    Hand(hand.faces.toList())
                }

            val walletsForRound =
                afterState.wallets.mapValues { (_, wallet) ->
                    wallet.copy(
                        currentBalance = wallet.currentBalance + afterState.game.ante
                    )
                }

            eventPublisher.publish(
                matchId,
                MatchEvent.RoundSummary(
                    roundNumber = completedRound.number,
                    winners = completedRound.winners ?: emptyList(),
                    prize = completedRound.pot,
                    wallets = walletsForRound,
                    playersAndCombinations = buildRoundHandsView(handsSnapshot)
                )
            )
        }


        else if (cmd is Command.FinishTurn || cmd is Command.NextRound) {

                eventPublisher.publish(
                    matchId, MatchEvent.TurnChange(
                        previousPlayer = prevState.game.playerOrder[prevState.game.currentPlayerIndex],
                        currentPlayer = afterState.game.playerOrder[afterState.game.currentPlayerIndex]
                    )
                )
            }
        }


        override fun createMatch(lobbyId: Int, totalRounds: Int, ante: Int): Either<MatchServiceError, Match> =
            trxManager.run {
                if (totalRounds <= 0 || ante < 0) {
                    return@run failure(MatchServiceError.InvalidState)
                }
                val lobby = repoLobby.findById(lobbyId) ?: return@run failure(MatchServiceError.LobbyNotFound)
                val match = repoMatch.createMatch(
                    lobbyId = lobbyId,
                    totalRounds = totalRounds,
                    ante = ante,
                    maxPlayers = lobby.maxPlayers
                )
                return@run success(match)
            }

        override fun getMatch(id: Int): Either<MatchServiceError, Match> = trxManager.run {
            val match = repoMatch.findById(id) ?: return@run failure(MatchServiceError.MatchNotFound)
            success(match)
        }

        override fun addPlayer(matchId: Int, player: MatchPlayer): Either<MatchServiceError, Boolean> = trxManager.run {
            val match = repoMatch.findById(matchId) ?: return@run failure(MatchServiceError.MatchNotFound)
            val currentPlayers = repoMatch.listPlayers(matchId)
            if (currentPlayers.any { it.userId == player.userId }) {
                return@run failure(MatchServiceError.PlayerAlreadyInMatch)
            }
            if (currentPlayers.size >= match.maxPlayers) {
                return@run failure(MatchServiceError.MatchFull)
            }
            val ok =
                repoMatch.addPlayer(matchId, player.userId, player.balanceAtStart, repoMatch.getMaxSeatNo(match.id) + 1)
            if (!ok) return@run failure(MatchServiceError.Unknown)
            success(true)
        }

        override fun removePlayer(matchId: Int, userId: Int): Either<MatchServiceError, Boolean> = trxManager.run {
            repoMatch.findById(matchId) ?: return@run failure(MatchServiceError.MatchNotFound)
            val players = repoMatch.listPlayers(matchId)
            if (players.none { it.userId == userId }) {
                return@run failure(MatchServiceError.PlayerNotFound)
            }
            val ok = repoMatch.removePlayer(matchId, userId)
            if (!ok) return@run failure(MatchServiceError.Unknown)
            success(true)
        }

        override fun updateState(matchId: Int, newState: MatchState): Either<MatchServiceError, Boolean> =
            trxManager.run {
                val match = repoMatch.findById(matchId) ?: return@run failure(MatchServiceError.MatchNotFound)
                if (match.state == MatchState.FINISHED) {
                    return@run failure(MatchServiceError.InvalidState)
                }
                val ok = repoMatch.updateState(matchId, newState)
                if (!ok) return@run failure(MatchServiceError.Unknown)
                success(true)
            }

        override fun listPlayers(matchId: Int): List<MatchPlayer> = trxManager.run {
            repoMatch.listPlayers(matchId)
        }

        override fun getBankedMatch(matchId: Int): BankedMatch? =
            matchManager.get(matchId)?.snapshot()

        override fun registerMatchEngine(engine: BankedGameMatchEngine) {
            matchManager.register(engine)
        }

        override fun registerBankedMatchFromDb(matchId: Int): Either<MatchServiceError, Boolean> {
            if (matchManager.get(matchId) != null) return success(true)

            val match = repoMatch.findById(matchId) ?: return failure(MatchServiceError.MatchNotFound)
            val players = repoMatch.listPlayers(matchId)

            val matchPlayers = players.map { player ->
                MatchPlayer(match.id, player.userId, player.seatNo, player.balanceAtStart)
            }

            val lobby = repoLobby.findById(match.lobbyId) ?: return failure(MatchServiceError.LobbyNotFound)
            val game = createNewGame(lobby, match, matchPlayers)

            val wallets = matchPlayers.associate { p ->
                val wallet = when (val walletResult = walletService.getWallet(p.userId)) {
                    is Success -> walletResult.value
                    is Failure -> return failure(MatchServiceError.PlayerNotFound)
                }
                p.userId to wallet
            }

            val banked = BankedMatch(match.id, game, wallets, null)

            val finalState = if (match.state != MatchState.RUNNING) {
                BankedMatchEngine.apply(banked, Command.Start(byUserId = lobby.lobbyHost))
            } else {
                banked
            }

            val engine = BankedGameMatchEngine(match.id, finalState)
            matchManager.register(engine)

            return success(true)
        }

        override fun getEventPublisher(): MatchEventPublisher = eventPublisher

}
