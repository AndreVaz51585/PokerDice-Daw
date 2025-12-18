package pt.isel.domain.user

data class Statistics(
    val userId: Int,
    val gamesPlayed: Int,
    val gamesWon: Int,
    val fiveOfAKind: Int,
    val fourOfAKind: Int,
    val fullHouse: Int,
    val straight: Int,
    val threeOfAKind: Int,
    val twoPair: Int,
    val onePair: Int,
    val bust: Int,
)
