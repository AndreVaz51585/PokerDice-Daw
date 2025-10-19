package pt.isel.repo.jdbi

import org.jdbi.v3.core.Handle
import org.jdbi.v3.core.kotlin.mapTo
import pt.isel.domain.Game.Match.Match
import pt.isel.domain.Game.Match.MatchPlayer
import pt.isel.domain.Game.Match.MatchState
import pt.isel.repo.RepositoryLobby
import pt.isel.repo.RepositoryMatch
import pt.isel.repo.jdbi.sql.MatchSql
import java.sql.Timestamp
import java.time.Instant
import java.util.*

/**
 * Implementação JDBI do repositório de Match.
 *
 * ATENÇÃO:
 * - Ajusta os nomes das tabelas e colunas aos do teu DDL real.
 * - Se user_id/lobby_id na BD forem INT em vez de Int, altera os tipos e binds.
 */
class RepositoryMatchJdbi(
    private val handle: Handle,
    private val repoLobby: RepositoryLobby
) : RepositoryMatch {

    override fun createMatch(
        lobbyId: Int,
        totalRounds: Int,
        ante: Int,
        state: MatchState,
        currentRoundNo: Int,
        startedAt: Instant,
        finishedAt: Instant?,
    ): Match {
        val matchId = handle.createUpdate(MatchSql.INSERT_MATCH)
            .bind("lobbyId", lobbyId)
            .bind("totalRounds", totalRounds)
            .bind("ante", ante)
            .bind("state", state.name) // Importante: converter o enum para string
            .bind("currentRoundNo", currentRoundNo)
            .bind("startedAt", startedAt)
            .bind("finishedAt", finishedAt)
            .executeAndReturnGeneratedKeys("id")
            .mapTo<Int>()
            .one()


        return Match(
            id = matchId,
            lobbyId = lobbyId,
            totalRounds = totalRounds,
            ante = ante,
            state = state, // ou o estado inicial correto
            currentRoundNo = currentRoundNo,
            startedAt = startedAt,
            finishedAt = finishedAt,
            rounds = emptyList() // TODO: carregar rounds quando houver esquema
        )
    }


    // ---------------------------
    // Leitura
    // ---------------------------
    override fun findById(id: Int): Match? {
        val partial = handle.createQuery(MatchSql.SELECT_MATCH)
            .bind("id", id)
            .map { rs, _ ->
                PartialMatchRow(
                    id = rs.getInt("id"),
                    lobbyId = rs.getInt("lobby_id"),
                    totalRounds = rs.getInt("total_rounds"),
                    ante = rs.getInt("ante"),
                    state = MatchState.valueOf(rs.getString("state")),
                    currentRoundNo = rs.getInt("current_round_no"),
                    startedAt = rs.getTimestamp("started_at").toInstant(),
                    finishedAt = rs.getTimestamp("finished_at")?.toInstant()
                )
            }
            .findOne()
            .orElse(null)
            ?: return null

        return Match(
            id = partial.id,
            lobbyId = partial.lobbyId,
            totalRounds = partial.totalRounds,
            ante = partial.ante,
            state = partial.state,
            currentRoundNo = partial.currentRoundNo,
            startedAt = partial.startedAt,
            finishedAt = partial.finishedAt,
            rounds = emptyList() // TODO: carregar rounds quando houver esquema
        )
    }

    override fun findAll(): List<Match> {
        val ids = handle.createQuery(MatchSql.SELECT_MATCHES_PAGED)
            .mapTo<Int>()
            .list()

        // N+1. Para volume elevado, considerar join + agregação.
        return ids.mapNotNull { findById(it) }
    }

    override fun exists(id: Int): Boolean =
        handle.createQuery(MatchSql.COUNT_EXISTS)
            .bind("id", id)
            .mapTo(Int::class.java)
            .one() > 0


    // ---------------------------
    // Atualizações
    // ---------------------------
    override fun updateState(id: Int, newState: MatchState, finishedAt: Instant?): Boolean =
        handle.createUpdate(MatchSql.UPDATE_STATE)
            .bind("id", id)
            .bind("state", newState.name)
            .bind("finishedAt", finishedAt?.let { Timestamp.from(it) })
            .execute() > 0

    override fun updateCurrentRound(id: Int, roundNo: Int): Boolean =
        handle.createUpdate(MatchSql.UPDATE_CURRENT_ROUND)
            .bind("id", id)
            .bind("roundNo", roundNo)
            .execute() > 0


    override fun save(entity: Match) {
        handle.createUpdate(MatchSql.UPDATE_MATCH)
            .bind("id", entity.id)
            .bind("lobbyId", entity.lobbyId)
            .bind("totalRounds", entity.totalRounds)
            .bind("ante", entity.ante)
            .bind("state", entity.state.name)
            .bind("currentRoundNo", entity.currentRoundNo)
            .bind("startedAt", Timestamp.from(entity.startedAt))
            .bind("finishedAt", entity.finishedAt?.let { Timestamp.from(it) })
            .execute()

    }

    // ---------------------------
    // Delete
    // ---------------------------
    override fun deleteById(id: Int): Boolean =
        // Elimina jogadores primeiro se não tiveres ON DELETE CASCADE
        handle.createUpdate(MatchSql.DELETE_MATCH)
            .bind("id", id)
            .execute() > 0


    override fun clear() {
        // Se houver FK, respeitar a ordem
        handle.createUpdate(MatchSql.CLEAR_MATCH_PLAYERS).execute()
        handle.createUpdate(MatchSql.CLEAR_MATCHES).execute()
    }

    // ---------------------------
    // Jogadores
    // ---------------------------

    override fun listPlayers(matchId: Int): List<MatchPlayer> {
        return handle.createQuery(MatchSql.SELECT_PLAYERS)
            .bind("matchId", matchId)
            .map { rs, _ ->
                MatchPlayer(
                    matchId = rs.getInt("match_id"),
                    userId = rs.getInt("user_id"),
                    seatNo = rs.getInt("seat_no"),
                    balanceAtStart = rs.getInt("balance_start"),
                    active = rs.getBoolean("active"),
                )
            }
            .list()
    }

    override fun setPlayerActive(matchId: Int, userId: Int, active: Boolean): Int {
        return handle.createUpdate(MatchSql.UPDATE_PLAYER_ACTIVE)
            .bind("matchId", matchId)
            .bind("userId", userId)
            .bind("active", active)
            .execute()
    }


    override fun addPlayer(
        matchId: Int,
        userId: Int,
        seatNo: Int,
        balanceAtStart: Int,
    ): Boolean {
        val rows = handle.createUpdate(MatchSql.INSERT_PLAYER)
            .bind("matchId", matchId)
            .bind("userId", userId)
            .bind("seatNo", UUID.randomUUID().hashCode()) // Geração simples de seatNo
            .bind("balanceStart", balanceAtStart)
            .execute() > 0
        return rows
    }

    override fun removePlayer(matchId: Int, userId: Int): Boolean =
        handle.createUpdate(MatchSql.DELETE_PLAYER)
            .bind("matchId", matchId)
            .bind("userId", userId)
            .execute() > 0

    override fun whoTurn(matchId: Int): Int? {
        return handle.createQuery(MatchSql.SELECT_WHO_TURN)
            .bind("matchId", matchId)
            .mapTo<Int>()
            .findOne()
            .orElse(null)
    }

    override fun setTurn(matchId: Int, userId: Int, turn: Boolean): Boolean {
        return handle.createUpdate(MatchSql.UPDATE_TURN)
            .bind("matchId", matchId)
            .bind("userId", userId)
            .bind("turn", turn)
            .execute() > 0
    }

    override fun getMaxSeatNo(matchId: Int): Int {
        return handle.createQuery(MatchSql.SELECT_MAX_SEAT)
            .bind("matchId", matchId)
            .mapTo<Int>()
            .one()
    }


    // ---------------------------
    // Helpers
    // ---------------------------
    private data class PartialMatchRow(
        val id: Int,
        val lobbyId: Int,
        val totalRounds: Int,
        val ante: Int,
        val state: MatchState,
        val currentRoundNo: Int,
        val startedAt: Instant,
        val finishedAt: Instant?
    )
}