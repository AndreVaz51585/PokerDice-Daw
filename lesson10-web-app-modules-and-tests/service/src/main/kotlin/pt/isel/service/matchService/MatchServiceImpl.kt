
import pt.isel.service.matchService.MatchManager
import pt.isel.service.matchService.MatchService
import pt.isel.service.matchService.MatchServiceError
import kotlinx.coroutines.runBlocking
import org.springframework.stereotype.Service
import pt.isel.domain.Game.Match.Match
import pt.isel.domain.Game.Match.MatchPlayer
import pt.isel.domain.Game.Match.MatchState
import pt.isel.domain.Game.money.BankedMatch
import pt.isel.domain.Game.money.BankedMatchEngine
import pt.isel.domain.Game.money.Wallet
import pt.isel.domain.Game.pokerDice.Command
import pt.isel.domain.Game.pokerDice.createNewGame
import pt.isel.repo.RepositoryLobby
import pt.isel.repo.RepositoryMatch
import pt.isel.repo.TransactionManager
import pt.isel.service.Auxiliary.Either
import pt.isel.service.Auxiliary.failure
import pt.isel.service.Auxiliary.success
import pt.isel.service.match.BankedGameMatchEngine
import kotlin.collections.get

@Service
class MatchServiceImpl(
    private val repoLobby: RepositoryLobby,
    private val repoMatch: RepositoryMatch,
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
        // Evitar duplicados iniciais
        val match = repoMatch.createMatch(
            lobbyId = lobbyId,
            totalRounds = totalRounds,
            ante = ante,
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
        // Return players from repo (table). If you rather use match.gameState.players,
        // ensure gameState is kept up to date on every player add/remove.
        repoMatch.listPlayers(matchId)
    }

    override fun getBankedMatch(matchId: Int): BankedMatch? =
        matchManager.get(matchId)?.snapshot()


    override fun applyCommand(matchId: Int, cmd: Command): Either<MatchServiceError, BankedMatch> {
        val engine = matchManager.get(matchId) ?: return failure(MatchServiceError.MatchNotFound)
        val res = runBlocking { engine.dispatch(cmd) } // mantém API síncrona; considere suspender controller
        return if (res.isSuccess) success(engine.snapshot()) else failure(MatchServiceError.Unknown)
    }

    override fun registerMatchEngine(engine: BankedGameMatchEngine) {
        matchManager.register(engine)

    }

    override fun registerBankedMatchFromDb(matchId: Int): Boolean {
        // Se já existir engine registrado, nada a fazer
        if (matchManager.get(matchId) != null) return true

        // O motor já foi criado no lobbyService.startMatch,
        // mas pode ter sido perdido (por exemplo, após reinicialização)
        try {
            val match = repoMatch.findById(matchId) ?: return false
            val players = repoMatch.listPlayers(matchId)

            // Reconstituir o estado do jogo
            val matchPlayers = players.map { player ->
                MatchPlayer(
                    matchId = match.id,
                    userId = player.userId,
                    seatNo = player.seatNo,
                    balanceAtStart = player.balanceAtStart
                )
            }

            // Buscar o lobby associado
            val lobby = repoLobby.findById(match.lobbyId) ?: return false

            // Criar o jogo
            val game = createNewGame(lobby, match, matchPlayers)

            // Criar wallets
            val wallets = matchPlayers.associate { p ->
                p.userId to Wallet(userId = p.userId, currentBalance = p.balanceAtStart)
            }

            // Criar BankedMatch
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

            return true
        } catch (t: Throwable) {
            return false
        }
    }


}
