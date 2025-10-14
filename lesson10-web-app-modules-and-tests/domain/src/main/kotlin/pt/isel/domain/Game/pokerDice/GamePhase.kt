package pt.isel.domain.Game.pokerDice


/** High level lifecycle of the match. */
enum class GamePhase {
    LOBBY,     // Players may join
    ROLLING,   // Active rounds in progress
    FINISHED   // All rounds completed
}
