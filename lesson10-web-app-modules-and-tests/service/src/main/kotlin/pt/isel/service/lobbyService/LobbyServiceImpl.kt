package pt.isel.service.lobbyService

import org.springframework.stereotype.Service
import pt.isel.domain.Game.Lobby.Lobby
import pt.isel.domain.Game.Lobby.LobbyState
import pt.isel.domain.Game.Match.MatchState
import pt.isel.domain.Game.money.Wallet
import pt.isel.domain.Game.pokerDice.Game
import pt.isel.domain.Game.pokerDice.PlayerState
import pt.isel.domain.user.User
import pt.isel.repo.RepositoryLobby
import pt.isel.repo.RepositoryUser
import pt.isel.repo.TransactionManager
import pt.isel.service.Auxiliary.Either
import pt.isel.service.Auxiliary.failure
import pt.isel.service.Auxiliary.success
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit


@Service
class LobbyServiceImpl(
    private val repoLobby: RepositoryLobby,
    private val repoUser: RepositoryUser,
    private val trxManager: TransactionManager,
) : LobbyService {

    // estrutura necessária ,implementada em memória para guardar o tempo de criação do lobby e assim podermos controlar o TimeOut
     private val lobbyTimeOuts = ConcurrentHashMap<Int, Long>()

    private val lobbyTimeOutMillis : Long = TimeUnit.SECONDS.toMillis(120) //TimeOut de 2 minutos

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
            if (repoUser.findById(hostId) == null) {
                return@run failure(LobbyServiceError.UserNotFound)
            }

            val lobby = repoLobby.createLobby(
                lobbyHostId = hostId,
                name = name,
                description = description,
                minPlayers = minPlayers,
                maxPlayers = maxPlayers,
                rounds = rounds,
                ante = ante
               // state = state
            )

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
        offset: Int
    ): List<Lobby> {
        val lobbies = repoLobby.listAllOpenLobbies(limit, offset)
        return lobbies
    }

    override fun joinLobby(lobbyId: Int, userId: Int): Either<LobbyServiceError, Int> =
        trxManager.run {
            val user = repoUser.findById(userId) ?: return@run failure(LobbyServiceError.UserNotFound)// verificação desnecessária porque o user já vem com autenticação feita caso contrário daria erro

            val lobby = repoLobby.findById(lobbyId) ?: return@run failure(LobbyServiceError.LobbyNotFound)

            if (lobby.state == LobbyState.FULL) {
                return@run failure(LobbyServiceError.LobbyFull)
            }

            val isAlreadyInLobby = repoLobby.listPlayers(lobbyId).any { it == user }

            if(isAlreadyInLobby) {
                return@run failure(LobbyServiceError.AlreadyInLobby)
            }

            val currentPlayers = repoLobby.countPlayers(lobbyId)
            val newPlayerCount = currentPlayers + 1

            val added = repoLobby.addPlayerToLobby(lobbyId, userId)

            if (!added) {
                return@run failure(LobbyServiceError.ErrorJoiningLobby)
            }

            val newState = updateLobbyStateIfNeeded(lobby, newPlayerCount)

            if(newState == LobbyState.FULL || newState == LobbyState.STARTED) return@run startMatch(lobbyId)



            return@run success(0) // retorna 0 se o jogador entrou mas o lobby não começou
        }

    override fun leaveLobby(lobbyId: Int, userId: Int) : Either<LobbyServiceError, Boolean> =
        trxManager.run {
            val lobby = repoLobby.findById(lobbyId) ?: return@run failure(LobbyServiceError.LobbyNotFound)
            val players = repoLobby.listPlayers(lobbyId)
            val userInLobby = players.any { it.id == userId }
            if (!userInLobby) {
                return@run failure(LobbyServiceError.UserIsNotInLobby)
            }

            val isHost = lobby.lobbyHost == userId && lobby.state == LobbyState.OPEN  // se o user a sair for o host e o lobby estiver OPEN então os jogadores são removidos e o lobby apagado

            if (isHost){
                // Remover todos os jogadores do lobby
                for (player in players) {
                    repoLobby.remove(lobbyId, player.id)
                }

                repoLobby.deleteById(lobbyId) // apaga consecutivamente o lobby
                lobbyTimeOuts.remove(lobbyId) // remove o TimeOut associado ao lobby
                return@run success(true)
            }

            val removed = repoLobby.remove(lobbyId, userId)
            if (!removed) {
                return@run failure(LobbyServiceError.ErrorLeavingLobby)
            }
            return@run success(true)
        }


    override fun getLobbyHost(lobby: Lobby): Either<LobbyServiceError, User> =
        trxManager.run {
            val host = repoLobby.getLobbyHost(lobby) ?: return@run failure(LobbyServiceError.UserNotFound)
            return@run success(host)
        }


    override fun listPlayers(lobbyId: Int): List<User> =
        trxManager.run {
            return@run repoLobby.listPlayers(lobbyId)
        }


    override fun startMatch(lobbyId: Int): Either<LobbyServiceError, Int> =
        trxManager.run {
            val lobby = repoLobby.findById(lobbyId) ?: return@run failure(LobbyServiceError.LobbyNotFound)
            val players = repoLobby.listPlayers(lobbyId)

            val initialPlayerStates = players.associate { player ->
                player.id to PlayerState(userId = player.id)
            }

            val initialGame = Game(
                id = 0, // se for gerado pelo repositório, mete 0 ou ignora se o construtor não o pedir
                hostId = lobby.lobbyHost,
                maxPlayers = lobby.maxPlayers,
                ante = lobby.ante,
                totalRounds = lobby.rounds,
                players = initialPlayerStates,
            )

            // Cria carteiras (wallets) iniciais
            val wallets = players.associate { player ->
                player.id.toLong() to Wallet(userId = player.id, currentBalance = 1000 - lobby.ante)
            }

            // 3️⃣ Cria o Match no repositório
            val matchResult = repoMatch.createMatch(
                lobbyId = lobbyId,
                totalRounds = lobby.rounds,
                ante = lobby.ante,
                gameState = initialGame,
                wallets = wallets,
                state = MatchState.RUNNING,
                currentRoundNo = 1,
                startedAt = java.time.Instant.now(),
                finishedAt = null,
                maxPlayers = lobby.maxPlayers
            )

            // 4️⃣ Atualiza estado do lobby
            repoLobby.save(lobby.copy(state = LobbyState.STARTED))
            lobbyTimeOuts.remove(lobbyId)

            return@run success(matchResult.id)
        }





    private fun updateLobbyStateIfNeeded(lobby: Lobby, playerCount: Int): LobbyState {
        val currentTime = System.currentTimeMillis()
        val timeout = lobbyTimeOuts[lobby.id] ?: (currentTime + lobbyTimeOutMillis)
        val hasExpired = currentTime >= timeout

        return when {
            playerCount >= lobby.maxPlayers ->{
                repoLobby.save(lobby.copy(state = LobbyState.FULL))
                LobbyState.FULL // se o numero de jogadores for igual ao maximo o estado passa a FULL
            }

            playerCount >= lobby.minPlayers && hasExpired ->{
                repoLobby.save(lobby.copy(state = LobbyState.STARTED))
                LobbyState.STARTED }// se o numero de jogadores for maior ou igual ao minimo e o tempo tiver expirado o estado passa a STARTED

            else -> LobbyState.OPEN // caso contrário mantem-se OPEN
        }
    }


}



