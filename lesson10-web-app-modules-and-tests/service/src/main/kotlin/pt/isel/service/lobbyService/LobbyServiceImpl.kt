package pt.isel.service.lobbyService

import jakarta.inject.Named
import pt.isel.domain.Game.Lobby.Lobby
import pt.isel.domain.Game.Lobby.LobbyState
import pt.isel.domain.user.AuthenticatedUser
import pt.isel.domain.user.User
import pt.isel.repo.RepositoryLobby
import pt.isel.repo.RepositoryUser
import pt.isel.repo.TransactionManager
import pt.isel.service.Auxiliary.Either
import pt.isel.service.Auxiliary.failure
import pt.isel.service.Auxiliary.success


@Named
class LobbyServiceImpl(
    private val repoLobby : RepositoryLobby,
    private val repoUser: RepositoryUser,
    private val trxManager: TransactionManager,
) : LobbyService {

    override fun createLobby(
        hostId: Int,
        name: String,
        description: String,
        minPlayers: Int,
        maxPlayers: Int,
        rounds: Int,
        ante: Int,
        state: LobbyState
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
               ante = ante,
               state = state
            )
            return@run success(lobby)
        }

        override fun getLobby(id: Int): Either<LobbyServiceError,Lobby> =
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

        override fun joinLobby(lobbyId: Int, userId: Int) : Either<LobbyServiceError,Boolean> =
         trxManager.run {
             val user = repoUser.findById(userId) ?: return@run failure(LobbyServiceError.UserNotFound)

             val lobby = repoLobby.findById(lobbyId) ?: return@run failure(LobbyServiceError.LobbyNotFound)

             if(lobby.state != LobbyState.OPEN) {
                 return@run failure(LobbyServiceError.LobbyClosed)
             }

                val currentPlayers = repoLobby.countPlayers(lobbyId)

                if(currentPlayers >= lobby.maxPlayers) {
                    return@run failure(LobbyServiceError.LobbyFull)
                }

             val added = repoLobby.addPlayerToLobby(lobbyId,userId)

             if(!added) {
                 return@run failure(LobbyServiceError.ErrorJoiningLobby)
             }

                return@run success(true)
         }

        override fun leaveLobby(lobbyId: Int, userId: Int) =
            trxManager.run {
                repoLobby.remove(lobbyId,userId)
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
    }


