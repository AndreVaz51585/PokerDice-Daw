package pt.isel.domain.Game.Match

data class MatchPlayer(
    val matchId: Int,
    val userId: Int,
    val seatNo: Int, // ordem fixa na mesa
    val balanceAtStart: Int, // saldo no início do match (para auditoria)
    val active: Boolean = true, // Está a jogar ou não
    val turn: Boolean = false,
)
