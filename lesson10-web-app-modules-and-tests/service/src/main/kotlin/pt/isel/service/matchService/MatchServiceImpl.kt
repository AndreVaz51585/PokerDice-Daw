import kotlinx.coroutines.runBlocking
import org.springframework.stereotype.Service
import pt.isel.domain.Game.Match.Match
import pt.isel.domain.Game.Match.MatchPlayer
import pt.isel.domain.Game.Match.MatchState
import pt.isel.domain.Game.money.BankedMatch
import pt.isel.domain.Game.money.BankedMatchEngine
import pt.isel.domain.Game.pokerDice.Command
import pt.isel.domain.Game.pokerDice.createNewGame
import pt.isel.repo.RepositoryLobby
import pt.isel.repo.RepositoryMatch
import pt.isel.repo.TransactionManager
import pt.isel.service.Auxiliary.*
import pt.isel.service.match.BankedGameMatchEngine
import pt.isel.service.matchService.MatchManager
import pt.isel.service.matchService.MatchService
import pt.isel.service.matchService.MatchServiceError
import pt.isel.service.walletService.WalletService

@Service
class MatchServiceImpl(
    private val repoLobby: RepositoryLobby,
    private val repoMatch: RepositoryMatch,
    private val walletService: WalletService,
    private val trxManager: TransactionManager,
    private val matchManager: MatchManager,
) : MatchService {


    override fun createMatch(
        lobbyId: Int,
        totalRounds: Int,
        ante: Int
    ): Either<MatchServiceError, Match> = trxManager.run {
        if (totalRounds <= 0 || ante < 0) {
            return@run failure(MatchServiceError.InvalidState)
        }

        val lobby = repoLobby.findById(lobbyId) ?: return@run failure(MatchServiceError.LobbyNotFound)
        // Evitar duplicados iniciais
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
        match.maxPlayers.let { max ->
            if (currentPlayers.size >= max) return@run failure(MatchServiceError.MatchFull)
        }
        val ok = repoMatch.addPlayer(
            matchId,
            userId = player.userId,
            balanceAtStart = player.balanceAtStart,
            seatNo = repoMatch.getMaxSeatNo(match.id) + 1
        )
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

        // Optional: update gameState to reflect removal
        success(true)
    }

    override fun updateState(matchId: Int, newState: MatchState): Either<MatchServiceError, Boolean> = trxManager.run {
        val match = repoMatch.findById(matchId) ?: return@run failure(MatchServiceError.MatchNotFound)

        // Simple validation: cannot change state once finished
        if (match.state == MatchState.FINISHED) {
            return@run failure(MatchServiceError.InvalidState)
        }

        // If transitioning to RUNNING you may want to initialize or start the embedded game.
        // Currently this method persists the state change; if you want the start to also
        // cause domain behaviour (e.g. collect antes via GameEngine), add that logic here.
        val ok = repoMatch.updateState(matchId, newState)
        if (!ok) return@run failure(MatchServiceError.Unknown)

        success(true)
    }

    override fun listPlayers(matchId: Int): List<MatchPlayer> = trxManager.run {
        repoMatch.listPlayers(matchId)
    }

    override fun getBankedMatch(matchId: Int): BankedMatch? =
        matchManager.get(matchId)?.snapshot()


    override fun applyCommand(matchId: Int, cmd: Command): Either<MatchServiceError, BankedMatch> {
        val engine = matchManager.get(matchId) ?: return failure(MatchServiceError.MatchNotFound)
        val res = runBlocking { engine.dispatch(cmd) }
        return if (res.isSuccess) success(engine.snapshot()) else failure(MatchServiceError.Unknown)
    }

    override fun registerMatchEngine(engine: BankedGameMatchEngine) {
        matchManager.register(engine)

    }

    override fun registerBankedMatchFromDb(matchId: Int): Either<MatchServiceError, Boolean> {

        if (matchManager.get(matchId) != null) return success(true)


        val match = repoMatch.findById(matchId) ?: return failure(MatchServiceError.MatchNotFound)
        val players = repoMatch.listPlayers(matchId)


        val matchPlayers = players.map { player ->
            MatchPlayer(
                matchId = match.id,
                userId = player.userId,
                seatNo = player.seatNo,
                balanceAtStart = player.balanceAtStart
            )
        }

        val lobby = repoLobby.findById(match.lobbyId) ?: return failure(MatchServiceError.LobbyNotFound)

        val game = createNewGame(lobby, match, matchPlayers)

        val wallets = matchPlayers.associate { p ->

            val walletResult = walletService.getWallet(
                p.userId,
                p.userId,
            )

            val wallet = when (walletResult) {
                is Success -> walletResult.value
                is Failure -> return failure(MatchServiceError.PlayerNotFound)
            }
            p.userId to wallet
        }

        val banked = BankedMatch(
            matchId = match.id,
            game = game,
            wallets = wallets,
            openPot = null
        )

        // Aplicar comando Start se necessário
        val finalState = if (match.state != MatchState.RUNNING) {
            BankedMatchEngine.apply(banked, Command.Start(byUserId = lobby.lobbyHost))
        } else {
            banked
        }

        // Registrar engine
        val engine = BankedGameMatchEngine(match.id, finalState)
        matchManager.register(engine)

        return success(true)
    }
}
