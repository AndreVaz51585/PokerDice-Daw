package pt.isel.domain.Game.Lobby

import java.util.*

data class Lobby(
    val id: UUID,
    val name: String,
    val description: String?,
    val lobbyHost: UUID,
    val minPlayers: Int,
    val maxPlayers: Int,
    val rounds: Int,
  //  val ante: Int,                          // número inteiro de “moedas” por ronda
  //  val players: List<LobbyPlayer>,       // lista de players (não apenas um número)
    val state: LobbyState = LobbyState.OPEN,
) {
    init {
        require(name.isNotBlank()) { " The lobby's name cannot be empty" }
        require(minPlayers in 2..maxPlayers) { "minPlayers must be between 2 and maxPlayers." }
        require(rounds >= 1) { "rounds must be  ≥ 1." }
    //    require(ante >= 0) { "ante não pode ser negativo." }
    }
}

