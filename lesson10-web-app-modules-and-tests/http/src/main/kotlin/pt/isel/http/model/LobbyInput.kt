package pt.isel.http.model

data class LobbyInput(
  val lobbyHostId: Int,
  val name: String,
  val description: String,
  val minPlayers: Int,
  val maxPlayers: Int,
  val rounds: Int,
  val ante: Int,
)

