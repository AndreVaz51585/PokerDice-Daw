package pt.isel.domain.Game

data class Hand (
    val faces :  List<Face>
) {
    init {
        require( faces.size == 5) {"One Hand must have exactly 5 faces!"}
    }

    private fun Hand.getCombination() : Combination = Combination.calculate(this)


    companion object {
        fun generateHand() : Hand = Hand(faces = List(5){Dice.roll()})
    }




}

