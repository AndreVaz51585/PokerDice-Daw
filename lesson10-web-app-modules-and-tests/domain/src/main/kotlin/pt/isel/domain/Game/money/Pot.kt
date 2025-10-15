package pt.isel.domain.Game.money

import java.time.Instant

data class Pot(
    val matchId: Long,
    val roundNumber: Int,
    val createdAt: Instant = Instant.now(),
    val contributions: Map<Long, Int> = emptyMap(), // userId -> total contributed
    val total: Int = 0,
    val closed: Boolean = false
) {

    init {
        require(matchId > 0) { "matchId inválido" }
        require(roundNumber > 0) { "roundNumber inválido" }
        require(total >= 0) { "Total do pote não pode ser negativo" }
        require(contributions.keys.all { it > 0 }) { "userId inválido em contributions" }
        require(contributions.values.all { it >= 0 }) { "Contribuições não podem ser negativas" }
        require(total == contributions.values.sum()) {
            "Total ($total) não coincide com a soma das contribuições (${contributions.values.sum()})"
        }
    }

    fun addContribution(userId: Long, amount: Int): Pot {
        require(!closed) { "Pote encerrado" }
        require(userId > 0) { "userId inválido" }
        require(amount > 0) { "Contribuição deve ser positiva" }
        val updated = contributions.toMutableMap()
        updated[userId] = (updated[userId] ?: 0) + amount
        return copy(
            contributions = updated.toMap(),
            total = total + amount
        )
    }

    fun close(): Pot {
        require(!closed) { "Pote já encerrado" }
        return copy(closed = true)
    }

    // Equal split across winners; odd chips go to lowest userIds deterministically.
    fun computeWinnerSplits(winnerUserIds: Set<Long>): Map<Long, Int> {
        require(closed) { "Pote deve estar encerrado para calcular distribuição" }
        require(winnerUserIds.isNotEmpty()) { "Winners não pode ser vazio" }
        require(winnerUserIds.all { it > 0 }) { "Winner ids inválidos" }

        val winners = winnerUserIds.sorted()
        val base = total / winners.size
        var remainder = total % winners.size

        val result = linkedMapOf<Long, Int>()
        for (id in winners) {
            val extra = if (remainder > 0) 1 else 0
            result[id] = base + extra
            if (remainder > 0) remainder--
        }
        return result
    }
}
