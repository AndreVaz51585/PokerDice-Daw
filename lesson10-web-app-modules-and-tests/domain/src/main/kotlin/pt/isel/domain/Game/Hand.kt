package pt.isel.domain.Game

data class Hand(
    val faces: List<Face>,
) {
    init {
        require(faces.size == 5) { "One Hand must have exactly 5 faces!" }
    }

    companion object {
        fun generateHand(): Hand = Hand(faces = List(5) { Dice.roll() })

        fun Hand.getCombination(): Pair<Combination, List<Face>> = Combination.calculate(this)
    }
}
