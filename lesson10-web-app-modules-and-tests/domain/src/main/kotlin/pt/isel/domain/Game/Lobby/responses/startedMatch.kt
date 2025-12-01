package pt.isel.domain.Game.Lobby.responses

data class startedMatch(
    val lobbyId : Int,
    val matchId : Int,
    val message : String,
)
