package pt.isel.repo

import pt.isel.domain.Game.Match.Match
import pt.isel.domain.Game.Match.MatchPlayer
import pt.isel.domain.Game.Match.MatchState
import java.time.Instant

/**
 * Domain port for Match persistence.
 * Keeps domain independent from infrastructure (JDBI, JDBC, etc.).
 *
 * NOTE:
 * - createMatch now accepts a fully formed Game (gameState) and optional wallets
 *   so the persisted Match contains the game state from the beginning.
 * - Implementations must (de)serialize the Game and Wallets (e.g. JSON in DB).
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
     *
     * Now accepts the initial gameState and optional wallets map so the persisted
     * Match contains the domain Game snapshot from the start.
     *
     * Returns the created Match (with its generated id if implementation assigns one).
     */
    fun createMatch(
        lobbyId: Int,
        totalRounds: Int,
        ante: Int,
        state: MatchState = MatchState.RUNNING,
        currentRoundNo: Int = 1,
        startedAt: Instant = Instant.now(),
        finishedAt: Instant? = null,
        maxPlayers: Int,
    ): Match

    /**
     * Save / upsert the given match (persist the updated gameState and wallets).
     * Returns true on success.
     */
    override fun save(entity: Match)

    /**
     * Updates the state and optionally the finishedAt timestamp.
     */
    fun updateState(
        id: Int,
        newState: MatchState,
        finishedAt: Instant? = null,
    ): Boolean

    /**
     * Updates the current round number.
     */
    fun updateCurrentRound(
        id: Int,
        roundNo: Int,
    ): Boolean

    // Player operations
    fun listPlayers(matchId: Int): List<MatchPlayer>


    fun setPlayerActive(
        matchId: Int,
        userId: Int,
        active: Boolean,
    ): Int

    fun addPlayer(
        matchId: Int,
        userId: Int,
        seatNo: Int,
        balanceAtStart: Int,
    ): Boolean

    fun removePlayer(
        matchId: Int,
        userId: Int,
    ): Boolean

    fun getMaxSeatNo(matchId: Int): Int
}
