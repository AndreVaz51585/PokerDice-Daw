package pt.isel.domain.Game.Match


import pt.isel.domain.Game.Round.Round
import java.time.Instant

data class Match(
    val id: Int,
    val lobbyId: Int,
    val ante: Int,
    val totalRounds: Int,
    val state: MatchState = MatchState.RUNNING,
    val startedAt: Instant = Instant.now(),
    val finishedAt: Instant? = null,
    val currentRoundNo: Int = 1,
    val rounds: List<Round> = emptyList(),
    val maxPlayers: Int
    ){
    init {
        require(totalRounds >= 1) { "totalRounds tem de ser ≥ 1." }
        require(ante >= 0) { "ante não pode ser negativo." }
        require(currentRoundNo in 1..totalRounds) { "currentRoundNo fora do intervalo [1, totalRounds]." }
    }
}
