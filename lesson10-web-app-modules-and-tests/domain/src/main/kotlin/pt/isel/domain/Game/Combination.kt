package pt.isel.domain.Game

import pt.isel.domain.Game.Hand.Companion.getCombination


/**
 * Poker Dice combination ranking.
 * Lower priority value => stronger hand.
 */
enum class Combination(val priority: Int) {
    FIVE_OF_A_KIND(1),
    FOUR_OF_A_KIND(2),
    FULL_HOUSE(3),
    STRAIGHT(4),
    THREE_OF_A_KIND(5),
    TWO_PAIR(6),
    PAIR(7),
    BUST(8);


    companion object {
        fun calculate(hand: Hand): Pair<Combination, List<Face>> {
            val faceCount = hand.faces.groupingBy { it }.eachCount()
            val sorted = hand.faces.sortedByDescending { it.priority }

            val combination = when {
                isFiveOfAKind(faceCount) -> FIVE_OF_A_KIND
                isFourOfAKind(faceCount) -> FOUR_OF_A_KIND
                isFullHouse(faceCount) -> FULL_HOUSE
                isStraight(sorted) -> STRAIGHT
                isThreeOfAKind(faceCount) -> THREE_OF_A_KIND
                isTwoPair(faceCount) -> TWO_PAIR
                isPair(faceCount) -> PAIR
                else -> BUST
            }
            return combination to sorted
        }

        private fun isFiveOfAKind(faceCounts: Map<Face, Int>) = faceCounts.size == 1

        private fun isFourOfAKind(faceCounts: Map<Face, Int>) = faceCounts.any { it.value == 4 }

        private fun isFullHouse(faceCounts: Map<Face, Int>) =
            faceCounts.values.contains(3) && faceCounts.values.contains(2)

        private fun isStraight(sortedFaces: List<Face>): Boolean {
            val priorities = sortedFaces.map { it.priority }
            return priorities.zipWithNext().all { (a, b) -> b == a - 1 }
        }

        private fun isThreeOfAKind(faceCounts: Map<Face, Int>) = faceCounts.any { it.value == 3 }

        private fun isTwoPair(faceCounts: Map<Face, Int>) = faceCounts.count { it.value == 2 } == 2

        private fun isPair(faceCounts: Map<Face, Int>) = faceCounts.any { it.value == 2 }

        /**
         * Compares two hands and returns the winning hand.
         * In case of a draw, returns the first hand.
         */
        fun compareHands(hand1: Hand, hand2: Hand): Hand {
            val (comb1, sorted1) = hand1.getCombination()
            val (comb2, sorted2) = hand2.getCombination()
            val combCompare = comb1.priority.compareTo(comb2.priority)
            return when {
                combCompare < 0 -> hand1
                combCompare > 0 -> hand2
                else -> {
                    for (i in sorted1.indices) {
                        val faceCompare = sorted1[i].priority.compareTo(sorted2[i].priority)
                        if (faceCompare < 0) return hand1
                        if (faceCompare > 0) return hand2
                    }
                    hand1 // draw fallback
                }
            }
        }

        enum class HandOutcome { HAND1, HAND2, DRAW }

        fun compareHandsOutcome(hand1: Hand, hand2: Hand): HandOutcome {
            val (comb1, sorted1) = hand1.getCombination()
            val (comb2, sorted2) = hand2.getCombination()
            val combCompare = comb1.priority.compareTo(comb2.priority)

            if (combCompare < 0) return HandOutcome.HAND1

            if (combCompare > 0) return HandOutcome.HAND2

            for (i in sorted1.indices) {
                val faceCompare = sorted1[i].priority.compareTo(sorted2[i].priority)

                if (faceCompare < 0) return HandOutcome.HAND1

                if (faceCompare > 0) return HandOutcome.HAND2
            }

            return HandOutcome.DRAW
        }

    }
}
