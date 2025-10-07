package pt.isel.domain.Game.money

import java.time.Instant


data class Ante(
    val amount: Int,
    val matchId: Long,
    val roundNumber: Int,
    val createdAt: Instant = Instant.now()
) {

    init {
        require(amount > 0) { "O valor do ante deve ser positivo (foi $amount)" }
    }
}