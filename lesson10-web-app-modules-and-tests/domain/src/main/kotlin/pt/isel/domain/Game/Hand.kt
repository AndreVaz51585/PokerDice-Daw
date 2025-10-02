package pt.isel.domain.Game

data class Hand(
    val dice :  List<Face>
) {
    init {
        require( dice.size == 5) {"One Hand must have exactly 5 faces!"}
    }

}

