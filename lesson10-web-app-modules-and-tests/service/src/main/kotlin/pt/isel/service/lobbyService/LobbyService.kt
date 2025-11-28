package pt.isel.service.lobbyService

import pt.isel.domain.Game.Lobby.Lobby
import pt.isel.domain.user.User
import pt.isel.service.Auxiliary.Either

interface LobbyService {

    fun createLobby(
        hostId: Int,
        name: String,
        description: String,
        minPlayers: Int,
        maxPlayers: Int,
        rounds: Int,
        ante: Int,
    ): Either<LobbyServiceError, Lobby>

    fun getLobby(id: Int): Either<LobbyServiceError, Lobby>

    fun listOpenLobbies(limit: Int, offset: Int): List<Lobby>

    fun joinLobby(lobbyId: Int, userId: Int): Either<LobbyServiceError, Int>

    fun leaveLobby(lobbyId: Int, userId: Int): Either<LobbyServiceError, Boolean>

    fun getLobbyHost(lobby: Lobby): Either<LobbyServiceError, User>

    fun listPlayers(lobbyId: Int): Either<LobbyServiceError, List<User>>

    fun startMatch(lobbyId: Int): Either<LobbyServiceError, Int?>

}