package pt.isel.repo.jdbi

import org.jdbi.v3.core.Handle
import org.jdbi.v3.core.kotlin.mapTo
import org.jdbi.v3.core.statement.StatementContext
import pt.isel.domain.Game.Face
import pt.isel.domain.Game.Hand
import pt.isel.domain.Game.Round.Round
import pt.isel.domain.Game.Round.RoundState
import pt.isel.repo.RepositoryRound
import pt.isel.repo.jdbi.sql.RoundSql
import java.sql.ResultSet
import java.time.Instant
import java.util.*

class RepositoryRoundJdbi(private val handle: Handle) : RepositoryRound {
    private class RoundMapper : (ResultSet, StatementContext) -> Round {
        override fun invoke(
            rs: ResultSet,
            ctx: StatementContext,
        ): Round {
            return Round(
                id = rs.getLong("id"),
                number = rs.getInt("number"),
                matchId = rs.getInt("match_id"),
                state =
                    when (rs.getString("status")) {
                        "IN_PROGRESS" -> RoundState.OPEN
                        "COMPLETED" -> RoundState.CLOSED
                        else -> RoundState.OPEN
                    },
                anteCoins = rs.getInt("ante_coins"),
                pot = rs.getInt("pot_coins"),
                hands = emptyMap(), // Mãos serão carregadas separadamente quando necessário
            )
        }
    }

    override fun createRound(
        matchId: Int,
        number: Int,
        anteCoins: Int,
        startedAt: Instant,
    ): Round {
        val generatedId =
            handle.createUpdate(RoundSql.INSERT_ROUND)
                .bind("matchId", matchId)
                .bind("number", number)
                .bind("anteCoins", anteCoins)
                .bind("potCoins", 0)
                .bind("startedAt", startedAt)
                .executeAndReturnGeneratedKeys("id")
                .mapTo<Long>()
                .one()

        return Round(
            id = generatedId,
            matchId = matchId,
            number = number,
            state = RoundState.OPEN,
            anteCoins = anteCoins,
            pot = 0,
            winners = null,
            hands = emptyMap(),
        )
    }

    override fun findById(id: Int): Round? {
        return handle.createQuery(RoundSql.SELECT_ROUND_BY_ID)
            .bind("id", id)
            .map(RoundMapper())
            .findOne()
            .orElse(null)
    }

    override fun findAll(): List<Round> {
        return handle.createQuery(RoundSql.SELECT_ALL_ROUNDS)
            .map(RoundMapper())
            .list()
    }

    override fun save(entity: Round) {
        if (findById(entity.id.toInt()) != null) {
            handle.createUpdate(RoundSql.UPDATE_ROUND)
                .bind("id", entity.id)
                .bind("number", entity.number)
                .bind("matchId", entity.matchId)
                .bind("anteCoins", entity.anteCoins)
                .bind(
                    "status",
                    when (entity.state) {
                        RoundState.OPEN -> "IN_PROGRESS"
                        RoundState.SCORING -> "IN_PROGRESS"
                        RoundState.CLOSED -> "COMPLETED"
                    },
                )
                .bind("potCoins", entity.pot)
                .bind("endedAt", if (entity.state == RoundState.CLOSED) Date() else null)
                .execute()
        } else {
            handle.createUpdate(RoundSql.INSERT_ROUND)
                .bind("id", entity.id)
                .bind("matchId", entity.matchId)
                .bind("number", entity.number)
                .bind("anteCoins", entity.anteCoins)
                .bind("potCoins", entity.pot)
                .bind("startedAt", Date())
                .execute()
        }
        for (userId in entity.winners ?: emptyList()) {
            handle.createUpdate(RoundSql.UPDATE_ROUND_WINNERS)
                .bind("roundId", entity.id)
                .bind("userId", userId)
                .execute()
        }
    }

    override fun findWinnersByRoundId(roundId: Long): List<Int> {
        return handle.createQuery(RoundSql.SELECT_ROUND_WINNERS)
            .bind("roundId", roundId)
            .mapTo<Int>()
            .list()
    }

    override fun deleteById(id: Int): Boolean {
        return handle.createUpdate(RoundSql.DELETE_ROUND)
            .bind("id", id)
            .execute() > 0
    }

    override fun clear() {
        handle.createUpdate(RoundSql.DELETE_ALL_ROUNDS)
            .execute()
    }

    override fun findByMatchId(matchId: Long): List<Round> {
        return handle.createQuery(RoundSql.SELECT_ROUNDS_BY_MATCH)
            .bind("matchId", matchId)
            .map(RoundMapper())
            .list()
    }

    override fun findCurrentRoundByMatchId(matchId: Long): Round? {
        return handle.createQuery(RoundSql.SELECT_CURRENT_ROUND_BY_MATCH)
            .bind("matchId", matchId)
            .map(RoundMapper())
            .findOne()
            .orElse(null)
    }

    override fun startRound(roundId: Long): Boolean {
        return handle.createUpdate(RoundSql.START_ROUND)
            .bind("id", roundId)
            .bind("startedAt", Date())
            .execute() > 0
    }

    override fun completeRound(
        roundId: Long,
        winnerUserId: Int,
    ): Boolean {
        return handle.createUpdate(RoundSql.COMPLETE_ROUND)
            .bind("id", roundId)
            .bind("status", "COMPLETED")
            .bind("endedAt", Date())
            .execute() > 0
    }

    override fun updateState(
        roundId: Long,
        newState: RoundState,
    ): Boolean {
        val sqlState =
            when (newState) {
                RoundState.OPEN -> "IN_PROGRESS"
                RoundState.SCORING -> "IN_PROGRESS"
                RoundState.CLOSED -> "COMPLETED"
            }

        return handle.createUpdate(RoundSql.UPDATE_ROUND_STATE)
            .bind("id", roundId)
            .bind("status", sqlState)
            .execute() > 0
    }

    override fun addToPot(
        roundId: Long,
        amount: Int,
    ): Boolean {
        val addPot = getPotAmount(roundId) + amount

        return handle.createUpdate(RoundSql.ADD_TO_POT)
            .bind("id", roundId)
            .bind("amount", addPot)
            .execute() > 0
    }

    override fun getPotAmount(roundId: Long): Int {
        return handle.createQuery(RoundSql.GET_POT_AMOUNT)
            .bind("id", roundId)
            .mapTo<Int>()
            .one()
    }

    override fun allPlayersPlayed(roundId: Long): Boolean {
        val count =
            handle.createQuery(RoundSql.COUNT_PLAYERS_PLAYED)
                .bind("id", roundId)
                .mapTo<Int>()
                .one()

        val totalPlayers =
            handle.createQuery(RoundSql.COUNT_TOTAL_PLAYERS)
                .bind("id", roundId)
                .mapTo<Int>()
                .one()

        return count == totalPlayers && totalPlayers > 0
    }

    override fun getRoundWithHands(roundId: Long): Round {
        val round =
            handle.createQuery(RoundSql.SELECT_ROUND_BY_ID)
                .bind("id", roundId)
                .map(RoundMapper())
                .findOne()
                .orElse(null)

        val hands =
            handle.createQuery(RoundSql.SELECT_HANDS_BY_ROUND)
                .bind("id", roundId)
                .map { rs: ResultSet, _: StatementContext ->
                    val userId = rs.getInt("user_id")

                    // Obter as faces dos dados da consulta SQL
                    val faces =
                        listOf(
                            enumValueOf<Face>(rs.getString("d1")),
                            enumValueOf<Face>(rs.getString("d2")),
                            enumValueOf<Face>(rs.getString("d3")),
                        )

                    // Criar o objeto Hand com as faces recuperadas
                    val hand = Hand(faces = faces)
                    Pair(userId, hand)
                }
                .list()
                .toMap()

        return round.copy(hands = hands)
    }

    override fun hasPlayerPlayed(
        roundId: Long,
        userId: Int,
    ): Boolean {
        return handle.createQuery(RoundSql.CHECK_PLAYER_PLAYED)
            .bind("id", roundId)
            .bind("userId", userId)
            .mapTo<Int>()
            .one() > 0
    }
}
