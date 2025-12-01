package pt.isel.domain.Game.Lobby

import pt.isel.domain.user.User
import java.util.*

data class Lobby(
    val id: Int,
    val name: String,
    val description: String?,
    val lobbyHost: Int,
    val minPlayers: Int,
    val maxPlayers: Int,
    val rounds: Int,
    val ante: Int,
    var state: LobbyState = LobbyState.OPEN,
) {
    init {
        require(name.isNotBlank()) { " The lobby's name cannot be empty" }
        require(minPlayers in 2..maxPlayers) { "minPlayers must be between 2 and maxPlayers." }
        require(rounds >= 1) { "rounds must be  ≥ 1." }
        require(ante >= 0) { "ante não pode ser negativo." }
    }
}

