package pt.isel.domain.Game.Lobby.Event

import pt.isel.domain.user.Player

data class playerLeft(
    val player: Player,
    val currentCount: Int,
)
