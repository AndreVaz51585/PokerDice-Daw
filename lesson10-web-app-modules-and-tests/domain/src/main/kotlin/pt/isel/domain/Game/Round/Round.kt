package pt.isel.domain.Game.Round

import pt.isel.domain.Game.Combination
import pt.isel.domain.Game.Face

/**
 * Round snapshot.
 * - number: sequential (>=1).
 * - pot: ante sum for this round.
 * - winners: playerIds who won (empty until scoring).
 * - hands: playerId -> (Combination, ordered faces) after showdown.
 * - state progression: OPEN -> SCORING -> (copied as CLOSED when moving to next round).
 */

data class Round(
    val number: Int,
    val state: RoundState = RoundState.OPEN,
    val pot: Int = 0,
    val winners: List<Int> = emptyList(),
    val hands: Map<Int, Pair<Combination, List<Face>>> = emptyMap()
) {
    init { require(number >= 1) { "number de round tem de ser ≥ 1." } }
}
