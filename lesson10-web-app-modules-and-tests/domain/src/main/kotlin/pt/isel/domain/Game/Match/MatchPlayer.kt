package pt.isel.domain.Game.Match

import java.util.*

data class MatchPlayer(
    val userId: UUID,
    val seatNo: Int,                // ordem fixa na mesa
    val balanceAtStart: Int,        // saldo no início do match (para auditoria)
    val active: Boolean = true      // consegue pagar o ante da próxima ronda?
)