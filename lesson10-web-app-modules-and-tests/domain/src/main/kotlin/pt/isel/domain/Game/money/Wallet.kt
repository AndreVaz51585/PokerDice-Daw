package pt.isel.domain.Game.money

import java.time.Instant

data class Wallet(
    val userId: Long,
    val currentBalance: Int,
    val lastUpdatedAt: Instant = Instant.now()
) {
    init {
        require(currentBalance >= 0) { "Saldo não pode ser negativo (valor atual: $currentBalance)" }
    }
}
