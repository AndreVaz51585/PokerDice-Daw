package pt.isel.domain.Game.Round

/** State inside each logical round. */
enum class RoundState {
    OPEN, // Players still taking turns
    SCORING, // Showdown computed, not yet advanced
    CLOSED, // Historical (past rounds)
}
