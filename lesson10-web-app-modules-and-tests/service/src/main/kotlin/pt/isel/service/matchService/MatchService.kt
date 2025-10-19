package pt.isel.service.matchService

import pt.isel.domain.Game.Match.Match
import pt.isel.domain.Game.Match.MatchPlayer
import pt.isel.domain.Game.Match.MatchState
import pt.isel.domain.Game.money.BankedMatch
import pt.isel.domain.Game.pokerDice.Command
import pt.isel.service.Auxiliary.Either
import pt.isel.service.match.BankedGameMatchEngine

interface MatchService {
    fun createMatch(
        lobbyId: Int,
        totalRounds: Int,
        ante: Int
    ): Either<MatchServiceError, Match>

    fun getMatch(id: Int): Either<MatchServiceError, Match>

    fun addPlayer(matchId: Int, player: MatchPlayer): Either<MatchServiceError, Boolean>

    fun removePlayer(matchId: Int, userId: Int): Either<MatchServiceError, Boolean>

    fun updateState(matchId: Int, newState: MatchState): Either<MatchServiceError, Boolean>

    fun listPlayers(matchId: Int): List<MatchPlayer>

    fun getBankedMatch(matchId: Int): BankedMatch?

    fun applyCommand(matchId: Int, cmd: Command): Either<MatchServiceError, BankedMatch>

    fun registerMatchEngine(engine: BankedGameMatchEngine)

    fun registerBankedMatchFromDb(matchId: Int): Boolean

}
