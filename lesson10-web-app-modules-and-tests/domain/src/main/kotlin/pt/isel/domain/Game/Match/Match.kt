package pt.isel.domain.Game.Match

import pt.isel.domain.Game.Round.Round
import java.time.Instant
import java.util.*
//TODO: Inutil
/**
 * Representa a partida já iniciada (derivada de um Lobby).
 * Responsabilidades: progresso do jogo (rondas/turnos), pote, resultados e encerramento.
 */
data class Match(
    val id: Int,
    val lobbyId: Int,                  // referência ao lobby que originou o match
    val players: List<MatchPlayer>,     // “snapshot” dos participantes no arranque, com ordem/seat
    val totalRounds: Int,
    val ante: Int,
    val state: MatchState = MatchState.RUNNING,
    val currentRoundNo: Int = 1,        // começa na 1ª ronda
    val startedAt: Instant = Instant.now(),
    val finishedAt: Instant? = null,
    val rounds: List<Round> = emptyList()
) {
    init {
        require(players.isNotEmpty()) { "Um match precisa de pelo menos 1 jogador." }
        require(players.map { it.seatNo }.distinct().size == players.size) { "Seats duplicados." }
        require(totalRounds >= 1) { "totalRounds tem de ser ≥ 1." }
        require(ante >= 0) { "ante não pode ser negativo." }
        require(currentRoundNo in 1..totalRounds) { "currentRoundNo fora do intervalo [1, totalRounds]." }
    }
}
