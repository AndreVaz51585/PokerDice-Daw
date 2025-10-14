package pt.isel.domain.Game.pokerDice


/** User intents processed by GameEngine. */
sealed interface Command {

    data class Join(val userId: Int) : Command

    data class Start(val byUserId: Int) : Command

    data class Hold(val userId: Int, val indices: Set<Int>) : Command

    data class Roll(val userId: Int) : Command

    data class FinishTurn(val userId: Int) : Command

    data class NextRound(val byUserId: Int) : Command

}
