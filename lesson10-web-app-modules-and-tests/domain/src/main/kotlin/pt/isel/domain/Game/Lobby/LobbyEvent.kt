package pt.isel.domain.Game.Lobby

import pt.isel.domain.user.Player

sealed class LobbyEvent {
    data class PlayerJoined(val player: Player, val currentCount: Int, val maxPlayers: Int) : LobbyEvent()

    data class PlayerLeft(val player: Player, val currentCount: Int) : LobbyEvent()

    data class MatchStarting(val matchId: Int) : LobbyEvent()

    data class LobbySnapshot(val lobby: Lobby, val players: List<Player>) : LobbyEvent()

    data class TimeoutUpdate(val remainingSeconds: Int) : LobbyEvent()

    object LobbyClosed : LobbyEvent()
}
