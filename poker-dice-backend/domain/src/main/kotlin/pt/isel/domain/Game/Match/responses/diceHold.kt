package pt.isel.domain.Game.Match.responses

import pt.isel.domain.Game.Face

data class DiceHold(
    val dices: List<Face>?,
    val heldIndices: Set<Int>,
    val rerollsLeft: Int,
)
