package pt.isel.domain.Game.money

import java.time.Instant

data class WalletTransaction(
    val id: Int,
    val userId: Int,
    val roundId: Int? = null,
    val amount: Int,  // negativo para débitos, positivo para créditos
    val createdAt: Instant = Instant.now()
)
