package pt.isel.http.model

data class MatchInput(
    val lobbyId: Int,
    val players: List<Int>,
    val totalRounds: Int,
    val ante: Int
)