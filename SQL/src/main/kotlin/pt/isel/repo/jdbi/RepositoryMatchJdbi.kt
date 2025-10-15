package pt.isel.repo.jdbi

import org.jdbi.v3.core.Handle
import org.jdbi.v3.core.kotlin.mapTo
import pt.isel.domain.Game.Match.Match
import pt.isel.domain.Game.Match.MatchPlayer
import pt.isel.domain.Game.Match.MatchState
import pt.isel.repo.RepositoryMatch
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
    private val handle: Handle
) : RepositoryMatch {

    // ---------------------------
    // SQL (ajusta ao teu DDL)
    // ---------------------------
    private object Sql {
        const val INSERT_MATCH = """
            INSERT INTO match (
                id, lobby_id, total_rounds, ante, state, current_round_no, started_at, finished_at
            ) VALUES (
                :id, :lobbyId, :totalRounds, :ante, CAST(:state AS match_state), :currentRoundNo, :startedAt, :finishedAt
            )
        """

        const val INSERT_PLAYER = """
            INSERT INTO match_player (
                match_id, user_id, seat_no, balance_at_start, active
            ) VALUES (
                :matchId, :userId, :seatNo, :balanceAtStart, :active
            )
            ON CONFLICT DO NOTHING
        """

        const val SELECT_MATCH = """
            SELECT id, lobby_id, total_rounds, ante, state, current_round_no, started_at, finished_at
            FROM match
            WHERE id = :id
        """

        const val SELECT_MATCHES_PAGED = """
            SELECT id
            FROM match
            ORDER BY started_at DESC, id
            LIMIT :limit OFFSET :offset
        """

        const val SELECT_PLAYERS = """
            SELECT user_id, seat_no, balance_at_start, active
            FROM match_player
            WHERE match_id = :matchId
            ORDER BY seat_no
        """

        const val UPDATE_STATE = """
            UPDATE match
            SET state = CAST(:state AS match_state),
                finished_at = :finishedAt
            WHERE id = :id
        """

        const val UPDATE_CURRENT_ROUND = """
            UPDATE match
            SET current_round_no = :roundNo
            WHERE id = :id
        """

        const val UPDATE_MATCH = """
            UPDATE match
            SET lobby_id = :lobbyId,
                total_rounds = :totalRounds,
                ante = :ante,
                state = CAST(:state AS match_state),
                current_round_no = :currentRoundNo,
                started_at = :startedAt,
                finished_at = :finishedAt
            WHERE id = :id
        """

        const val DELETE_MATCH = """
            DELETE FROM match
            WHERE id = :id
        """

        const val DELETE_PLAYERS_BY_MATCH = """
            DELETE FROM match_player
            WHERE match_id = :matchId
        """

        const val DELETE_PLAYER = """
            DELETE FROM match_player
            WHERE match_id = :matchId AND user_id = :userId
        """

        const val UPDATE_PLAYER_ACTIVE = """
            UPDATE match_player
            SET active = :active
            WHERE match_id = :matchId AND user_id = :userId
        """

        const val COUNT_EXISTS = """
            SELECT COUNT(*) FROM match WHERE id = :id
        """

        const val CLEAR_MATCH_PLAYERS = "DELETE FROM match_player"
        const val CLEAR_MATCHES = "DELETE FROM match"
    }

    // ---------------------------
    // Criação
    // ---------------------------
    override fun createMatch(id: Int, lobbyId: Int, players: List<MatchPlayer>, totalRounds: Int, ante: Int): Match {
        handle.createUpdate(Sql.INSERT_MATCH)
            .bind("id", id)
            .bind("lobbyId", lobbyId)
            .bind("totalRounds", totalRounds)
            .bind("ante", ante)
            .execute()

        if (players.isNotEmpty()) {
            val batch = handle.prepareBatch(Sql.INSERT_PLAYER)
            players.forEach { p ->
                batch
                    .bind("matchId", id)
                    .bind("userId", p.userId)
                    .bind("seatNo", p.seatNo)
                    .bind("balanceAtStart", p.balanceAtStart)
                    .bind("active", p.active)
                    .add()
            }
            batch.execute()
        }

        return Match(
            id = id,
            lobbyId = lobbyId,
            players = players,
            totalRounds = totalRounds,
            ante = ante,
            state = MatchState.RUNNING, // ou o estado inicial correto
            currentRoundNo = 1,
            startedAt = Instant.now(),
            finishedAt = null,
            rounds = emptyList()
        )

    }

    // ---------------------------
    // Leitura
    // ---------------------------
    override fun findById(id: Int): Match? {
        val partial = handle.createQuery(Sql.SELECT_MATCH)
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

        val players = listPlayers(id)

        return Match(
            id = partial.id,
            lobbyId = partial.lobbyId,
            players = players,
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
        val ids = handle.createQuery(Sql.SELECT_MATCHES_PAGED)
            .mapTo<Int>()
            .list()

        // N+1. Para volume elevado, considerar join + agregação.
        return ids.mapNotNull { findById(it) }
    }

    override fun exists(id: Int): Boolean =
        handle.createQuery(Sql.COUNT_EXISTS)
            .bind("id", id)
            .mapTo(Int::class.java)
            .one() > 0

    // ---------------------------
    // Atualizações
    // ---------------------------
    override fun updateState(id: Int, newState: MatchState, finishedAt: Instant?): Boolean =
        handle.createUpdate(Sql.UPDATE_STATE)
            .bind("id", id)
            .bind("state", newState.name)
            .bind("finishedAt", finishedAt?.let { Timestamp.from(it) })
            .execute() > 0

    override fun updateCurrentRound(id: Int, roundNo: Int): Boolean =
        handle.createUpdate(Sql.UPDATE_CURRENT_ROUND)
            .bind("id", id)
            .bind("roundNo", roundNo)
            .execute() > 0


    override fun save(entity: Match) {
        handle.createUpdate(Sql.UPDATE_MATCH)
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
        handle.createUpdate(Sql.DELETE_PLAYERS_BY_MATCH)
            .bind("matchId", id)
            .execute() > 0


    override fun clear() {
        // Se houver FK, respeitar a ordem
        handle.createUpdate(Sql.CLEAR_MATCH_PLAYERS).execute()
        handle.createUpdate(Sql.CLEAR_MATCHES).execute()
    }

    // ---------------------------
    // Jogadores
    // ---------------------------

    override fun listPlayers(matchId: Int): List<MatchPlayer> {
        return handle.createQuery(Sql.SELECT_PLAYERS)
            .bind("matchId", matchId)
            .map { rs, _ ->
                MatchPlayer(
                    userId = rs.getInt("user_id"),
                    seatNo = rs.getInt("seat_no"),
                    balanceAtStart = rs.getInt("balance_at_start"),
                    active = rs.getBoolean("active")
                )
            }
            .list()
    }


    override fun addPlayer(matchId: Int, player: MatchPlayer): Boolean {
        val rows = handle.createUpdate(Sql.INSERT_PLAYER)
            .bind("matchId", matchId)
            .bind("userId", player.userId)
            .bind("seatNo", player.seatNo)
            .bind("balanceAtStart", player.balanceAtStart)
            .bind("active", player.active)
            .execute() > 0
        return rows
    }

    override fun removePlayer(matchId: Int, userId: Int): Boolean =
        handle.createUpdate(Sql.DELETE_PLAYER)
            .bind("matchId", matchId)
            .bind("userId", userId)
            .execute() > 0

    override fun setPlayerActive(matchId: Int, userId: Int, active: Boolean): Int =
        handle.createUpdate(Sql.UPDATE_PLAYER_ACTIVE)
            .bind("matchId", matchId)
            .bind("userId", userId)
            .bind("active", active)
            .execute()

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