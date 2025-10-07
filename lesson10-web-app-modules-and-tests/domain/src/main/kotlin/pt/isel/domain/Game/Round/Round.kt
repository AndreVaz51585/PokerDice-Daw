package pt.isel.domain.Game.Round


data class Round(
    val number: Int,
    val state: RoundState = RoundState.OPEN,
    val pot: Int = 0,                           // soma dos antes pagos nesta ronda
    val turns: List<Int> = emptyList(),
    val results: List<String> = emptyList()     // preenchido em SCORING/CLOSED
) {
    init { require(number >= 1) { "number de round tem de ser ≥ 1." } }
}