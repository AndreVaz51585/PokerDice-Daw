package pt.isel.domain.Game

import java.time.Instant
import java.util.*

data class Lobbie (val id: Int,val numPlayers: Int, val rounds: Int) {

}

data class Lobby(
    val id: UUID,
    val name: String,
    val description: String?,
    val hostUserId: UUID,
    val minPlayers: Int,
    val maxPlayers: Int,
    val rounds: Int,
    val ante: Int,                          // número inteiro de “moedas” por ronda
    val players: List<LobbyPlayer>,         // lista de adesões (não apenas um número)
    val state: LobbyState = LobbyState.OPEN,
) {
    init {
        require(name.isNotBlank()) { "O nome do lobby não pode ser vazio." }
        require(minPlayers in 2..maxPlayers) { "minPlayers deve estar entre 2 e maxPlayers." }
        require(rounds >= 1) { "rounds tem de ser ≥ 1." }
        require(ante >= 0) { "ante não pode ser negativo." }
    }
}

enum class LobbyState { OPEN, STARTED, CLOSED }

enum class LobbyPlayerRole { HOST, PLAYER }
