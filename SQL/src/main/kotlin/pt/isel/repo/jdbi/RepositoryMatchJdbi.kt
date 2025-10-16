package pt.isel.repo.jdbi

import org.jdbi.v3.core.Handle
import org.jdbi.v3.core.kotlin.mapTo
import pt.isel.domain.Game.Match.Match
import pt.isel.domain.Game.Match.MatchPlayer
import pt.isel.domain.Game.Match.MatchState
import pt.isel.domain.user.User
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
    private val handle: Handle
) : RepositoryMatch {

    override fun createMatch(
        lobbyId: Int,
        players: List<User>,
        totalRounds: Int,
        ante: Int,
        state: MatchState,
        currentRoundNo: Int,
        startedAt: Instant,
        finishedAt: Instant?
    ): Match {

      val id =  handle.createUpdate(MatchSql.INSERT_MATCH)
            .bind("lobbyId", lobbyId)
            .bind("totalRounds", totalRounds)
            .bind("ante", ante)
            .bind("state", state.name) // Importante: converter o enum para string
            .bind("currentRoundNo", currentRoundNo)
            .bind("startedAt", startedAt)
            .bind("finishedAt", finishedAt)
            .executeAndReturnGeneratedKeys()
            .mapTo(Int::class.java)
            .one()

        if (players.isNotEmpty()) {
            val batch = handle.prepareBatch(MatchSql.INSERT_PLAYER)
            players.forEach { p ->
                batch
                    .bind("matchId", id)
                    .bind("userId", p.id)
                    .bind("seatNo", p.seatNo) // deveria ser serial não algo definido por nós?
                    .bind("balanceAtStart", p.balanceAtStart) // valor?
                    .bind("active", p.active) // não se sabe logo deve ser inativo para todos inicialmente
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
        handle.createUpdate(MatchSql.DELETE_PLAYERS_BY_MATCH)
            .bind("matchId", id)
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
                    userId = rs.getInt("user_id"),
                    seatNo = rs.getInt("seat_no"),
                    balanceAtStart = rs.getInt("balance_at_start"),
                    active = rs.getBoolean("active")
                )
            }
            .list()
    }


    override fun addPlayer(matchId: Int, player: MatchPlayer): Boolean {
        val rows = handle.createUpdate(MatchSql.INSERT_PLAYER)
            .bind("matchId", matchId)
            .bind("userId", player.userId)
            .bind("seatNo", player.seatNo)
            .bind("balanceAtStart", player.balanceAtStart)
            .bind("active", player.active)
            .execute() > 0
        return rows
    }

    override fun removePlayer(matchId: Int, userId: Int): Boolean =
        handle.createUpdate(MatchSql.DELETE_PLAYER)
            .bind("matchId", matchId)
            .bind("userId", userId)
            .execute() > 0

    override fun setPlayerActive(matchId: Int, userId: Int, active: Boolean): Int =
        handle.createUpdate(MatchSql.UPDATE_PLAYER_ACTIVE)
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