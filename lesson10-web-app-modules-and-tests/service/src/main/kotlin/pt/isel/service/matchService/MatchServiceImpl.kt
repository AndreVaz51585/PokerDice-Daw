import kotlinx.coroutines.runBlocking
import org.springframework.stereotype.Service
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
import kotlin.compareTo
import kotlin.text.get

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

            eventPublisher.publish(
                matchId, MatchEvent.RoundSummary(
                    roundNumber = lastRound.number,
                    winners = lastRound.winners,
                    prize = lastRound.pot,
                    wallets = afterState.wallets,
                    playersAndCombinations = lastRound.hands
                )
            )


            val match = repoMatch.findById(matchId) ?: return
            repoMatch.save(
                match.copy(
                    state = MatchState.FINISHED,
                    finishedAt = java.time.Instant.now()
                )
            )
            val lobbyPlayers = repoLobby.listPlayers(match.lobbyId)
            for(player in lobbyPlayers) {

                repoLobby.remove(lobbyId = match.lobbyId ,player.id)
                val wallet = afterState.wallets[player.id] ?: continue
                walletService.update(
                    Wallet(
                    userId = player.id,
                    currentBalance = wallet.currentBalance
                )
                )
            }

            val roundWins = mutableMapOf<Int, Int>()
            afterState.game.rounds.forEach { round ->
                round.winners?.forEach { winnerId ->
                    roundWins[winnerId] = (roundWins[winnerId] ?: 0) + 1
                }
            }

            val winner = roundWins.maxByOrNull { it.value }?.key


            eventPublisher.publish(
                matchId, MatchEvent.GameEndPayload(
                    winner = winner!!,
                    wallets = afterState.wallets
                )
            )
            return // Não publicar mais eventos
        }


        if (roundEnded) {



            val completedRoundIndex = afterState.game.rounds.size - 2
            if (completedRoundIndex >= 0) {
                val completedRound = afterState.game.rounds[completedRoundIndex]


                val walletsForRound = if (completedRound.number == 1) {
                    // PROBLEMA QUE NOS LEVA A SOLUÇÃO HARDCODED ABAIXO:
                    // quando tentamos obter o sado do prevstate.wallets da primeira ronda validamos que o mesmo se apresenta com o ante já deduzido e sem prémios associados
                    // caso tentassemos o afterState já teria o prémio da primeira ronda aplicado e a ante da ronda 2 deduzida o que não é o que queremos
                    // Solução hardcoded: reverter a dedução do ante para todos os jogadores na primeira ronda.
                    afterState.wallets.mapValues { (_, wallet) ->
                        wallet.copy(currentBalance = wallet.currentBalance + afterState.game.ante)
                    }
                } else {
                    // Rondas seguintes → usando prevState (antes de pagar nova ante)
                    afterState.wallets.mapValues { (_,wallet) -> wallet.copy(currentBalance = wallet.currentBalance + afterState.game.ante) }
                }

                eventPublisher.publish(
                    matchId, MatchEvent.RoundSummary(
                        roundNumber = completedRound.number,
                        winners = completedRound.winners,
                        prize = completedRound.pot,
                        wallets = walletsForRound,
                        playersAndCombinations = completedRound.hands
                    )
                )
            }


            val ante = afterState.game.ante
            val playersAbleToPay = afterState.wallets.count { it.value.currentBalance >= ante }

            if (playersAbleToPay <= 1) {

                eventPublisher.publish(
                    matchId, MatchEvent.RoundSummary(
                        roundNumber = lastRound.number,
                        winners = lastRound.winners,
                        prize = lastRound.pot,
                        wallets = afterState.wallets,
                        playersAndCombinations = lastRound.hands
                    )
                )


                val roundWins = mutableMapOf<Int, Int>()
                afterState.game.rounds.forEach { round ->
                    round.winners?.forEach { winnerId ->
                        roundWins[winnerId] = (roundWins[winnerId] ?: 0) + 1
                    }
                }

                val winner = roundWins.maxByOrNull { it.value }?.key

                eventPublisher.publish(
                    matchId, MatchEvent.GameEndPayload(
                        winner = winner!!,
                        wallets = afterState.wallets
                    )
                )
            } else {

                eventPublisher.publish(
                    matchId, MatchEvent.MatchSnapshot(
                        matchId = matchId,
                        currentRoundNumber = afterState.game.rounds.size,
                        totalRounds = afterState.game.totalRounds,
                        playerOrder = afterState.game.playerOrder,
                        currentPlayer = afterState.game.playerOrder[afterState.game.currentPlayerIndex]
                    )
                )
            }
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
