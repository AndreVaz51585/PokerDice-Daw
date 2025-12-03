package pt.isel.domain.Game.GlobalLobby

import pt.isel.domain.Game.Lobby.Lobby

sealed class GlobalLobbyEvent{

    data class lobbyCreated(val lobby: Lobby): GlobalLobbyEvent()

    data class lobbyRemoved(val lobbyId: Int): GlobalLobbyEvent()

}