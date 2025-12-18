package pt.isel.domain.Game.pokerDice

import pt.isel.domain.Game.Combination
import pt.isel.domain.Game.Face
import pt.isel.domain.Game.Hand
import pt.isel.domain.Game.Hand.Companion.getCombination

/**
 * Pure showdown helper: evaluates all players' 5 dice and returns:
 * - winners list
 * - hands map playerId -> (Combination, ordered faces)
 */
object Showdown {
    private data class HandValue(val combination: Combination, val ordered: List<Face>) : Comparable<HandValue> {
        override fun compareTo(other: HandValue): Int {
            // Lower priority means stronger (per Combination.priority design)
            val c = combination.priority.compareTo(other.combination.priority)

            if (c != 0) return -c

            for (i in ordered.indices) {
                val cmp = ordered[i].priority.compareTo(other.ordered[i].priority)

                if (cmp != 0) return -cmp
            }

            return 0
        }
    }

    data class Result(
        val winners: List<Int>,
        val hands: Map<Int, Pair<Combination, List<Face>>>,
    )

    fun resolve(dicePerPlayer: Map<Int, List<Face>>): Result {
        val hands =
            dicePerPlayer.mapValues { (_, dice) ->
                val (comb, ordered) = Hand(dice).getCombination()
                comb to ordered
            }
        val values = hands.mapValues { HandValue(it.value.first, it.value.second) }
        val best = values.maxBy { it.value }.value
        val winners = values.filter { it.value == best }.keys.sorted()
        return Result(winners, hands)
    }
}
