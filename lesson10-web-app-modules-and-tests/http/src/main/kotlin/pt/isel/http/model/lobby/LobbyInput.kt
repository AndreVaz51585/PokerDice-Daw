package pt.isel.http.model.lobby

data class LobbyInput(
  val name: String,
  val description: String,
  val minPlayers: Int,
  val maxPlayers: Int,
  val rounds: Int,
  val ante: Int,
)