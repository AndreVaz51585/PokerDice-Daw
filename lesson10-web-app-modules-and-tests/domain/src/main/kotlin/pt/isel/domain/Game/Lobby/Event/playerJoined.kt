package pt.isel.domain.Game.Lobby.Event

import pt.isel.domain.user.Player

data class playerJoined(
   val player : Player,
   val currentCount : Int,
   val maxPlayers : Int
)
