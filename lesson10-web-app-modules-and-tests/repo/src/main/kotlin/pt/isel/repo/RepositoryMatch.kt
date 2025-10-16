package pt.isel.repo

import pt.isel.domain.Game.Match.Match
import pt.isel.domain.Game.Match.MatchPlayer
import pt.isel.domain.Game.Match.MatchState
import java.time.Instant

/**
 * Domain port for Match persistence.
 * Keeps domain independent from infrastructure (JDBI, JDBC, etc.).
 */
interface RepositoryMatch : Repository<Match> {

    /**
     * Returns the Match by id or null if not found.
     */
    override fun findById(id: Int): Match?

    /**
     * Checks if a Match exists.
     */
    fun exists(id: Int): Boolean

    /**
     * Creates a Match and its initial players (transaction handled externally).
     */
    fun createMatch(
        lobbyId: Int,
        totalRounds: Int,
        ante: Int,
        state: MatchState = MatchState.RUNNING,
        currentRoundNo: Int = 1,
        startedAt: Instant = Instant.now(),
        finishedAt: Instant? = null
    ): Match


    /**
     * Updates the state and optionally the finishedAt timestamp.
     */
    fun updateState(
        id: Int, newState: MatchState, finishedAt: Instant? = null
    ): Boolean

    /**
     * Updates the current round number.
     */
    fun updateCurrentRound(id: Int, roundNo: Int): Boolean

    // Player operations
    fun listPlayers(matchId: Int): List<MatchPlayer>
    fun setPlayerActive(matchId: Int, userId: Int, active: Boolean): Int
    fun addPlayer(
        matchId: Int,
        userId: Int,
        seatNo: Int,
        balanceAtStart: Int,
    ): Boolean
    fun removePlayer(matchId: Int, userId: Int): Boolean
    fun whoTurn(matchId: Int): Int?
    fun setTurn(matchId: Int, userId: Int, turn: Boolean): Boolean
    fun getMaxSeatNo(matchId: Int) : Int
}
