package pt.isel.service.lobbyService

import pt.isel.domain.Game.Lobby.Lobby
import pt.isel.domain.Game.Lobby.LobbyState

class LobbyService {

    fun canJoinLobby(lobby: Lobby?, currentPlayers: List<Int>): Boolean {

        if (lobby == null || lobby.state != LobbyState.OPEN) return false

        if (currentPlayers.size >= lobby.maxPlayers) return false

        return true
    }
}