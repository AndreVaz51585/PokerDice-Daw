package pt.isel.service.lobbyService

import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import pt.isel.domain.Game.GlobalLobby.GlobalLobbyEvent
import pt.isel.domain.Game.Lobby.Lobby
import pt.isel.domain.Game.Lobby.LobbyEvent
import pt.isel.domain.Game.Lobby.LobbyState
import pt.isel.domain.Game.Match.Match
import pt.isel.domain.Game.Match.MatchPlayer
import pt.isel.domain.Game.money.BankedMatch
import pt.isel.domain.Game.money.BankedMatchEngine
import pt.isel.domain.Game.money.Wallet
import pt.isel.domain.Game.pokerDice.Command
import pt.isel.domain.Game.pokerDice.createNewGame
import pt.isel.domain.user.Player
import pt.isel.domain.user.User
import pt.isel.repo.RepositoryLobby
import pt.isel.repo.RepositoryUser
import pt.isel.repo.TransactionManager
import pt.isel.service.Auxiliary.*
import pt.isel.service.lobbyService.GlobalLobby.GlobalLobbyEventPublisher
import pt.isel.service.lobbyService.Lobby.LobbyEventPublisher
import pt.isel.service.match.BankedGameMatchEngine
import pt.isel.service.matchService.MatchManager
import pt.isel.service.statisticsService.StatisticsService
import pt.isel.service.walletService.WalletService
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

@Service
class LobbyServiceImpl(
    private val repoLobby: RepositoryLobby,
    private val repoUser: RepositoryUser,
    private val trxManager: TransactionManager,
    private val matchManager: MatchManager,
    private val walletService: WalletService,
    private val statisticsService: StatisticsService,
    private val lobbyeventPublisher: LobbyEventPublisher,
    private val globaleventPublisher: GlobalLobbyEventPublisher,
) : LobbyService {
    // estrutura necessária ,implementada em memória para guardar o tempo de criação do lobby e assim podermos controlar o TimeOut
    private val lobbyTimeOuts = ConcurrentHashMap<Int, Long>()

    private val lobbyTimeOutMillis: Long = TimeUnit.SECONDS.toMillis(120) // TimeOut de 2 minutos

    @Scheduled(fixedDelay = 5000) // executa a cada 5 segundos
    fun checkLobbyTimeouts() {
        val openLobbies = repoLobby.findAll().filter { it.state == LobbyState.OPEN }

        for (lobby in openLobbies) {
            val players = repoLobby.listPlayers(lobby.id)
            val newState = updateLobbyStateIfNeeded(lobby, players.size)

            if (newState == LobbyState.STARTED) {
                trxManager.run {
                    val matchIdResult = startMatch(lobby.id)
                    if (matchIdResult is Success) {
                        lobbyeventPublisher.publish(lobby.id, LobbyEvent.MatchStarting(matchIdResult.value))
                    }
                }
            }
        }
    }

    override fun createLobby(
        hostId: Int,
        name: String,
        description: String,
        minPlayers: Int,
        maxPlayers: Int,
        rounds: Int,
        ante: Int,
        // state: LobbyState
    ): Either<LobbyServiceError, Lobby> =

        trxManager.run {
            // numa fase inicial, só vamos verificar se o host existe se sim continuamos caso contrártio retornamos o erro
            val host = repoUser.findById(hostId) ?: return@run failure(LobbyServiceError.UserNotFound)

            val hostAmount = walletService.getAmount(host.id, host.id)

            if (hostAmount is Success && hostAmount.value < ante) {
                return@run failure(LobbyServiceError.NotEnoughMoney)
            }

            val allLobbies = repoLobby.findAll()

            val isInAnotherLobby =
                allLobbies.any { lobby ->
                    repoLobby.listPlayers(lobby.id).any { it.id == host.id }
                }

            if (isInAnotherLobby) {
                return@run failure(LobbyServiceError.UserAlreadyInAnotherLobby)
            }

            val lobby =
                repoLobby.createLobby(
                    lobbyHostId = hostId,
                    name = name,
                    description = description,
                    minPlayers = minPlayers,
                    maxPlayers = maxPlayers,
                    rounds = rounds,
                    ante = ante,
                    // state = state
                )

            val added = repoLobby.addPlayerToLobby(lobby.id, lobby.lobbyHost)
            if (!added) return@run failure(LobbyServiceError.ErrorJoiningLobby)

            globaleventPublisher.publish(GlobalLobbyEvent.lobbyCreated(lobby))

            // adicionar contador para definir TimeOut para o lobby Começar caso tenho o numero minimo de jogadores
            lobbyTimeOuts[lobby.id] = System.currentTimeMillis() + lobbyTimeOutMillis

            return@run success(lobby)
        }

    override fun getLobby(id: Int): Either<LobbyServiceError, Lobby> =
        trxManager.run {
            val lobby = repoLobby.findById(id) ?: return@run failure(LobbyServiceError.LobbyNotFound)
            return@run success(lobby)
        }

    override fun listOpenLobbies(
        limit: Int,
        offset: Int,
    ): List<Lobby> {
        val lobbies = repoLobby.listAllOpenLobbies(limit, offset)
        return lobbies
    }

    override fun joinLobby(
        lobbyId: Int,
        userId: Int,
    ): Either<LobbyServiceError, Int> =
        trxManager.run {
            val user =
                repoUser.findById(userId)
                    ?: return@run failure(LobbyServiceError.UserNotFound) // verificação desnecessária porque o user já vem com autenticação feita caso contrário daria erro

            val userAmount = walletService.getAmount(userId, user.id)

            val lobby = repoLobby.findById(lobbyId) ?: return@run failure(LobbyServiceError.LobbyNotFound)

            if (userAmount is Success && userAmount.value < lobby.ante) {
                return@run failure(LobbyServiceError.NotEnoughMoney)
            }

            if (lobby.state == LobbyState.FULL) {
                return@run failure(LobbyServiceError.LobbyFull)
            }

            val isAlreadyInLobby = repoLobby.listPlayers(lobbyId).any { it == user }

            if (isAlreadyInLobby) {
                return@run failure(LobbyServiceError.AlreadyInLobby)
            }

            val allLobbies = repoLobby.findAll()

            val isInAnotherLobby =
                allLobbies.any { lobby ->
                    repoLobby.listPlayers(lobby.id).any { it.id == user.id }
                }

            if (isInAnotherLobby) {
                return@run failure(LobbyServiceError.UserAlreadyInAnotherLobby)
            }

            val added = repoLobby.addPlayerToLobby(lobbyId, userId)

            if (!added) {
                return@run failure(LobbyServiceError.ErrorJoiningLobby)
            }

            val currentPlayers = repoLobby.listPlayers(lobbyId)

            lobbyeventPublisher.publish(
                lobbyId,
                LobbyEvent.PlayerJoined(
                    player = Player(user.id, user.name),
                    currentCount = currentPlayers.size,
                    maxPlayers = lobby.maxPlayers,
                ),
            )

            val newState = updateLobbyStateIfNeeded(lobby, currentPlayers.size)

            if (newState == LobbyState.FULL || newState == LobbyState.STARTED) {
                val matchIdResult = startMatch(lobbyId)
                return@run when (matchIdResult) {
                    is Success -> {
                        val matchId = matchIdResult.value
                        lobbyeventPublisher.publish(lobbyId, LobbyEvent.MatchStarting(matchId))
                        success(matchId)
                    }
                    is Failure -> failure(LobbyServiceError.ErrorCreatingMatch)
                }
            }

            return@run success(-1) // retorna -1 se o jogador entrou mas o lobby não começou
        }

    override fun leaveLobby(
        lobbyId: Int,
        userId: Int,
    ): Either<LobbyServiceError, Boolean> =
        trxManager.run {
            val lobby = repoLobby.findById(lobbyId) ?: return@run failure(LobbyServiceError.LobbyNotFound)
            val players = repoLobby.listPlayers(lobbyId)
            val userInLobby = players.any { it.id == userId }
            if (!userInLobby) {
                return@run failure(LobbyServiceError.UserIsNotInLobby)
            }

            val user =
                repoUser.findById(userId)
                    ?: return@run failure(LobbyServiceError.UserNotFound)

            val isHost =
                lobby.lobbyHost == userId && lobby.state == LobbyState.OPEN // se o user a sair for o host e o lobby estiver OPEN então os jogadores são removidos e o lobby apagado

            if (isHost) {

                lobbyeventPublisher.publish(
                    lobbyId,
                    LobbyEvent.HostLeft(message ="Host has left the lobby. The lobby will be closed.")
                )


                // Remover todos os jogadores do lobby
                for (player in players) {
                    repoLobby.remove(lobbyId, player.id)
                }

                repoLobby.deleteById(lobbyId) // apaga consecutivamente o lobby
                lobbyTimeOuts.remove(lobbyId) // remove o TimeOut associado ao lobby

                globaleventPublisher.publish(GlobalLobbyEvent.lobbyRemoved(lobbyId))
                return@run success(true)
            }

            val removed = repoLobby.remove(lobbyId, userId)
            if (!removed) {
                return@run failure(LobbyServiceError.ErrorLeavingLobby)
            }
            val currentPlayers = repoLobby.listPlayers(lobbyId)

            lobbyeventPublisher.publish(
                lobbyId,
                LobbyEvent.PlayerLeft(
                    player = Player(user.id, user.name),
                    currentCount = currentPlayers.size,
                ),
            )

            return@run success(true)
        }

    override fun getLobbyHost(lobby: Lobby): Either<LobbyServiceError, User> =
        trxManager.run {
            val host = repoLobby.getLobbyHost(lobby) ?: return@run failure(LobbyServiceError.UserNotFound)
            return@run success(host)
        }

    override fun listPlayers(lobbyId: Int): Either<LobbyServiceError, List<User>> =
        trxManager.run {
            val list = repoLobby.listPlayers(lobbyId)
            if (list.isEmpty()) {
                return@run failure(LobbyServiceError.LobbyNotFound)
            }
            return@run success(list)
        }

    override fun startMatch(lobbyId: Int): Either<LobbyServiceError, Int> =
        trxManager.run {
            val lobby = repoLobby.findById(lobbyId) ?: return@run failure(LobbyServiceError.LobbyNotFound)
            val players = repoLobby.listPlayers(lobbyId)

            val matchResult: Match =
                repoMatch.createMatch(
                    lobbyId = lobbyId,
                    totalRounds = lobby.rounds,
                    ante = lobby.ante,
                    maxPlayers = lobby.maxPlayers,
                ) // retorna a match ou MatchServiceError


            // 2) Transfere jogadores do Lobby para a Match e constrói MatchPlayer com balanceAtStart
            val matchPlayers = mutableListOf<MatchPlayer>()
            for (user in players) {
                val amountOutcome = walletService.getAmount(user.id, user.id)

                val balanceAtStart =
                    when (amountOutcome) {
                        is Failure -> return@run failure(LobbyServiceError.ErrorJoiningLobby)
                        is Success -> amountOutcome.value
                    }

                // persistir jogador na match (assume repoMatch.addPlayer retorna boolean)
                val seatNo = repoMatch.getMaxSeatNo(matchResult.id) + 1
                val added =
                    repoMatch.addPlayer(
                        matchResult.id,
                        user.id,
                        seatNo,
                        balanceAtStart,
                    )
                if (!added) {
                    return@run failure(LobbyServiceError.ErrorJoiningLobby)
                }
                statisticsService.incrementGamesPlayed(user.id)

                matchPlayers.add(
                    MatchPlayer(
                        matchId = matchResult.id,
                        userId = user.id,
                        seatNo = seatNo,
                        balanceAtStart = balanceAtStart,
                    ),
                )
            }

            // Cria o Game de domínio com os matchPlayers
            val game = createNewGame(lobby, matchResult, matchPlayers)

            // Constrói wallets iniciais (Int userId -> Wallet) a partir de matchPlayers
            val wallets: Map<Int, Wallet> =
                matchPlayers.associate { mp ->
                    mp.userId to Wallet(userId = mp.userId, currentBalance = mp.balanceAtStart)
                }

            val banked =
                BankedMatch(
                    matchId = matchResult.id,
                    game = game,
                    wallets = wallets,
                    openPot = null,
                )

            val started = BankedMatchEngine.apply(banked, Command.Start(byUserId = lobby.lobbyHost))
            val engine = BankedGameMatchEngine(matchResult.id, started, statisticsService)
            matchManager.register(engine)
            return@run success(matchResult.id)
        }

    private fun updateLobbyStateIfNeeded(
        lobby: Lobby,
        playerCount: Int,
    ): LobbyState {
        val currentTime = System.currentTimeMillis()
        val timeout = lobbyTimeOuts[lobby.id] ?: (currentTime + lobbyTimeOutMillis)
        val hasExpired = currentTime >= timeout

        return when {
            playerCount == lobby.maxPlayers -> {
                repoLobby.save(lobby.copy(state = LobbyState.FULL))
                lobbyeventPublisher.publish(lobby.id, LobbyEvent.LobbyClosed) // anucia que o lobby está cheio e portanto fechado
                LobbyState.FULL // se o numero de jogadores for igual ao maximo o estado passa a FULL
            }

            playerCount >= lobby.minPlayers && hasExpired -> {
                repoLobby.save(lobby.copy(state = LobbyState.STARTED))
                LobbyState.STARTED
            } // se o numero de jogadores for maior ou igual ao minimo e o tempo tiver expirado o estado passa a STARTED

            else -> LobbyState.OPEN // caso contrário mantem-se OPEN
        }
    }

    override fun getLobbyEventPublisher(): LobbyEventPublisher = lobbyeventPublisher

    override fun getGlobalLobbyEventPublisher() = globaleventPublisher
}
