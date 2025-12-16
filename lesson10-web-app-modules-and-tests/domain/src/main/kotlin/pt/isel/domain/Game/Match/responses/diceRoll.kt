package pt.isel.domain.Game.Match.responses

import pt.isel.domain.Game.Face

data class DiceRoll(
    val dices: List<Face>?,
    val rerollsLeft: Int,
    val hand: String,
)
