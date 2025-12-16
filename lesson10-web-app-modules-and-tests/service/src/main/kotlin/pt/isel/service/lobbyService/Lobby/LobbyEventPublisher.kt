package pt.isel.service.lobbyService.Lobby

import pt.isel.domain.Game.Lobby.LobbyEvent

interface LobbyEventPublisher {
    fun publish(
        lobbyId: Int,
        event: LobbyEvent,
    )

    fun subscribe(lobbyId: Int, listener: (LobbyEvent) -> Unit): () -> Unit // retorna unsubscribe
}
