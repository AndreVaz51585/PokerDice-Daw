package pt.isel.service.lobbyService.GlobalLobby

import pt.isel.domain.Game.GlobalLobby.GlobalLobbyEvent

interface GlobalLobbyEventPublisher{

    fun publish(event: GlobalLobbyEvent)

    fun subscribe(listener: (GlobalLobbyEvent) -> Unit): () -> Unit // retorna unsubscribe

}

