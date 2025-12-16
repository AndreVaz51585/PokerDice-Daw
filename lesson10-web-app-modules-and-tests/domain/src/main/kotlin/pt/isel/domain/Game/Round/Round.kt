package pt.isel.domain.Game.Round

import pt.isel.domain.Game.Hand

/**
 * Round snapshot.
 * - number: sequential (>=1).
 * - pot: ante sum for this round.
 * - winners: playerIds who won (empty until scoring).
 * - hands: playerId -> (Combination, ordered faces) after showdown.
 * - state progression: OPEN -> SCORING -> (copied as CLOSED when moving to next round).
 */

data class Round(
    val id: Long = 0,
    val number: Int,
    val matchId: Int,
    val state: RoundState = RoundState.OPEN,
    val anteCoins: Int,
    val pot: Int = 0,
    val winners: List<Int>? = emptyList(),
    val hands: Map<Int, Hand> = emptyMap(),
) {
    init {
        require(number >= 1) { "number de round tem de ser ≥ 1." }
    }
}
