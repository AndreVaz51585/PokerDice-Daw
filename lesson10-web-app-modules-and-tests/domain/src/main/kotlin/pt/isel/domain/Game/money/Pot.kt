package pt.isel.domain.Game.money

import java.time.Instant


data class Pot(
    val matchId: Long,
    val roundNumber: Int,
    val createdAt: Instant = Instant.now(),
    val contributions: Map<Long, Int> = emptyMap(), // userId -> total contribuído
    val total: Int = 0,
    val closed: Boolean = false
) {

    init {
        require(matchId > 0) { "matchId inválido" }
        require(roundNumber > 0) { "roundNumber inválido" }
        require(total >= 0) { "Total do pote não pode ser negativo" }
    }
}