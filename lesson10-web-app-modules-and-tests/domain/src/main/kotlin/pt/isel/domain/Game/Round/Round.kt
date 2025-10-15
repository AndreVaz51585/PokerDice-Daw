package pt.isel.domain.Game.Round

import pt.isel.domain.Game.Combination
import pt.isel.domain.Game.Face
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
    val id: Long,
    val matchId: Long,
    val number: Int,
    val state: RoundState = RoundState.OPEN,
    val anteCoins: Int,
    val potCoins: Int = 0,
    val winnerUserId: Int? = null,
    val hands: Map<Int, Hand> = emptyMap()
){
    init { require(number >= 1) { "Round number must be >= 1." } }
}