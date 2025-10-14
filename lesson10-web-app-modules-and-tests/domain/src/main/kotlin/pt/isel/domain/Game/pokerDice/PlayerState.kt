package pt.isel.domain.Game.pokerDice

import pt.isel.domain.Game.Face


/**
 * Per player transient turn state.
 * - dice: current 5 dice (empty until first Roll).
 * - held: indices (0..4) the player keeps on rerolls.
 * - rerollsLeft: remaining rerolls (starts at 2 => up to 3 total rolls).
 * - done: true after FinishTurn.
 */
data class PlayerState(
    val userId: Int,
    val dice: List<Face> = emptyList(),
    val held: Set<Int> = emptySet(),
    val rerollsLeft: Int = 2,
    val done: Boolean = false
)
