package pt.isel.domain.Game.Lobby.Event

import pt.isel.domain.Game.Lobby.Lobby
import pt.isel.domain.user.Player

data class lobbySnapshot(
    val lobby: Lobby,
    val players: List<Player>,
    val currentCount: Int,
)
