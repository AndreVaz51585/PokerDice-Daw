package pt.isel.repo

import pt.isel.domain.Game.Lobby.Lobby
import pt.isel.domain.Game.Lobby.LobbyState
import pt.isel.domain.user.User

interface RepositoryLobby : Repository<Lobby> {

    fun createLobby(
        lobbyHostId: Int,
        name: String,
        description: String,
        minPlayers: Int,
        maxPlayers: Int,
        rounds: Int,
        ante: Int,
        state: LobbyState
    ): Lobby


    fun getLobbyHost(lobby: Lobby): User?

    fun listAllOpenLobbies(
        limit: Int,
        offset: Int
    ): List<Lobby>


    fun addPlayerToLobby(lobbyId: Int, playerId: Int): Boolean

    fun remove(lobbyId: Int, userId: Int): Int

    fun listPlayers(lobbyId: Int): List<User>

    fun countPlayers(lobbyId: Int): Int

}