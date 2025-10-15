package pt.isel.service.lobbyService

import pt.isel.domain.Game.Match.Match
import pt.isel.domain.Game.Match.MatchPlayer
import pt.isel.domain.Game.Match.MatchState
import pt.isel.service.Auxiliary.Either

interface MatchService {
    fun createMatch(
        id: Int,
        lobbyId: Int,
        players: List<MatchPlayer>,
        totalRounds: Int,
        ante: Int
    ): Either<MatchServiceError, Match>

    fun getMatch(id: Int): Either<MatchServiceError, Match>

    fun addPlayer(matchId: Int, player: MatchPlayer): Either<MatchServiceError, Boolean>

    fun removePlayer(matchId: Int, userId: Int): Either<MatchServiceError, Boolean>

    fun updateState(matchId: Int, newState: MatchState): Either<MatchServiceError, Boolean>

    fun listPlayers(matchId: Int): List<MatchPlayer>
}
