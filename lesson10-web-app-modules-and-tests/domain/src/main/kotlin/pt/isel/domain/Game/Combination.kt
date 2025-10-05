package pt.isel.domain.Game

enum class Combination(priority : Int){
    FIVE_OF_A_KIND(1),
    FOUR_OF_A_KIND(2),
    FULL_HOUSE(3),
    STRAIGHT(4),
    THREE_OF_A_KIND(5),
    TWO_PAIR(6),
    PAIR(7),
    BUST(8);


    companion object {
        fun calculate(hand: Hand): Combination {

            val faceCount =
                hand.faces.groupingBy { it }.eachCount()    // necessário para sabermos o numero de elemntos repetidos
            val sorted =
                hand.faces.sortedByDescending { it.priority } // ordena as faces por prioridade de forma crescente

            return when {
                isFiveOfAKind(faceCount) -> FIVE_OF_A_KIND
                isFourOfAKind(faceCount) -> FOUR_OF_A_KIND
                isFullHouse(faceCount) -> FULL_HOUSE
                isStraight(sorted) -> STRAIGHT
                isThreeOfAKind(faceCount) -> THREE_OF_A_KIND
                isTwoPair(faceCount) -> TWO_PAIR
                isPair(faceCount) -> PAIR
                else -> BUST
            }
        }

        private fun isFiveOfAKind(faceCounts: Map<Face, Int>): Boolean = faceCounts.size == 1

        private fun isFourOfAKind(faceCounts: Map<Face, Int>): Boolean = faceCounts.any { it.value == 4 }

        private fun isFullHouse(faceCounts: Map<Face, Int>): Boolean = faceCounts.values.contains(3) && faceCounts.values.contains(2)

        private fun isStraight(sortedFaces: List<Face>): Boolean {
            val priorities = sortedFaces.map { it.priority }
            return priorities.zipWithNext().all { (a, b) -> b == a + 1 }
        }

        private fun isThreeOfAKind(faceCounts: Map<Face, Int>): Boolean = faceCounts.any { it.value == 3 }

        private fun isTwoPair(faceCounts: Map<Face, Int>): Boolean = faceCounts.count { it.value == 2 } == 2

        private fun isPair(faceCounts: Map<Face, Int>): Boolean = faceCounts.any { it.value == 2 }

    }

    }



