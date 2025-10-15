package pt.isel.domain.Game

/**
 * Nota: menor priority => face mais forte (ACE é 1).
 */
enum class Face(val priority: Int) {
    ACE(1),
    KING(2),
    QUEEN(3),
    JACK(4),
    TEN(5),
    NINE(6);
}
