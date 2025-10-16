/*package pt.isel.repo.jdbi

import org.jdbi.v3.core.Jdbi
import org.jdbi.v3.core.kotlin.mapTo
import org.jdbi.v3.core.statement.StatementContext
import pt.isel.domain.Game.Face
import pt.isel.domain.Game.Hand
import pt.isel.domain.Game.Round.Round
import pt.isel.domain.Game.Round.RoundState
import pt.isel.repo.RepositoryRound
import pt.isel.repo.jdbi.sql.RoundSql
import java.sql.ResultSet
import java.util.*

class RepositoryRoundJdbi(private val jdbi: Jdbi) : RepositoryRound {

    private class RoundMapper : (ResultSet, StatementContext) -> Round {
        override fun invoke(rs: ResultSet, ctx: StatementContext): Round {
            return Round(
                number = rs.getInt("number"),
                matchId = rs.getLong("match_id"),
                state = when (rs.getString("status")) {
                    "IN_PROGRESS" -> RoundState.OPEN
                    "COMPLETED" -> RoundState.CLOSED
                    else -> RoundState.OPEN
                },
                anteCoins = rs.getInt("ante_coins"),
                pot = rs.getInt("pot_coins"),
                winners = rs.getString("winner_user_id")
                    ?.split(",")
                    ?.filter { it.isNotBlank() }
                    ?.map { it.trim().toInt() },
                hands = emptyMap() // Mãos serão carregadas separadamente quando necessário
            )
        }
    }

    override fun findById(id: Int): Round? {
        return jdbi.withHandle<Round?, Exception> { handle ->
            handle.createQuery(RoundSql.SELECT_ROUND_BY_ID)
                .bind("id", id)
                .map(RoundMapper())
                .findOne()
                .orElse(null)
        }
    }

    override fun findAll(): List<Round> {
        return jdbi.withHandle<List<Round>, Exception> { handle ->
            handle.createQuery(RoundSql.SELECT_ALL_ROUNDS)
                .map(RoundMapper())
                .list()
        }
    }

    override fun save(entity: Round) {
        jdbi.useHandle<Exception> { handle ->
            if (findById(entity.id.toInt()) != null) {
                handle.createUpdate(RoundSql.UPDATE_ROUND)
                    .bind("id", entity.id)
                    .bind("number", entity.number)
                    .bind("match_id", entity.matchId)
                    .bind("ante_coins", entity.anteCoins)
                    .bind("status", when (entity.state) {
                        RoundState.OPEN -> "IN_PROGRESS"
                        RoundState.SCORING -> "IN_PROGRESS"
                        RoundState.CLOSED -> "COMPLETED"
                    })
                    .bind("pot_coins", entity.pot)
                    .bind("winner_user_id", entity.winners)
                    .bind("ended_at", if (entity.state == RoundState.CLOSED) Date() else null)
                    .execute()
            } else {
                handle.createUpdate(RoundSql.INSERT_ROUND)
                    .bind("id", entity.id)
                    .bind("match_id", entity.matchId)
                    .bind("number", entity.number)
                    .bind("ante_coins", entity.anteCoins)
                    .bind("pot_coins", entity.pot)
                    .bind("winner_user_id", entity.winners)
                    .bind("started_at", Date())
                    .execute()
            }
        }
    }

    override fun deleteById(id: Int): Boolean {
        return jdbi.withHandle<Boolean, Exception> { handle ->
            val rowsAffected = handle.createUpdate(RoundSql.DELETE_ROUND)
                .bind("id", id)
                .execute()
            rowsAffected > 0
        }
    }

    override fun clear() {
        jdbi.useHandle<Exception> { handle ->
            handle.createUpdate(RoundSql.DELETE_ALL_ROUNDS)
                .execute()
        }
    }

    override fun findByMatchId(matchId: Long): List<Round> {
        return jdbi.withHandle<List<Round>, Exception> { handle ->
            handle.createQuery(RoundSql.SELECT_ROUNDS_BY_MATCH)
                .bind("match_id", matchId)
                .map(RoundMapper())
                .list()
        }
    }

    override fun findCurrentRoundByMatchId(matchId: Long): Round? {
        return jdbi.withHandle<Round?, Exception> { handle ->
            handle.createQuery(RoundSql.SELECT_CURRENT_ROUND_BY_MATCH)
                .bind("match_id", matchId)
                .map(RoundMapper())
                .findOne()
                .orElse(null)
        }
    }

    override fun startRound(roundId: Long): Boolean {
        return jdbi.withHandle<Boolean, Exception> { handle ->
            val rowsAffected = handle.createUpdate(RoundSql.START_ROUND)
                .bind("id", roundId)
                .bind("started_at", Date())
                .execute()
            rowsAffected > 0
        }
    }

    override fun completeRound(roundId: Long, winnerUserId: Int): Boolean {
        return jdbi.withHandle<Boolean, Exception> { handle ->
            val rowsAffected = handle.createUpdate(RoundSql.COMPLETE_ROUND)
                .bind("id", roundId)
                .bind("winner_user_id", winnerUserId)
                .bind("status", "COMPLETED")
                .bind("ended_at", Date())
                .execute()
            rowsAffected > 0
        }
    }

    override fun updateState(roundId: Long, newState: RoundState): Boolean {
        val sqlState = when (newState) {
            RoundState.OPEN -> "IN_PROGRESS"
            RoundState.SCORING -> "IN_PROGRESS"
            RoundState.CLOSED -> "COMPLETED"
        }

        return jdbi.withHandle<Boolean, Exception> { handle ->
            val rowsAffected = handle.createUpdate(RoundSql.UPDATE_ROUND_STATE)
                .bind("id", roundId)
                .bind("status", sqlState)
                .execute()
            rowsAffected > 0
        }
    }

    override fun addToPot(roundId: Long, amount: Int): Int {
        return jdbi.withHandle<Int, Exception> { handle ->
            handle.createUpdate(RoundSql.ADD_TO_POT)
                .bind("id", roundId)
                .bind("amount", amount)
                .execute()

            handle.createQuery(RoundSql.GET_POT_AMOUNT)
                .bind("id", roundId)
                .mapTo<Int>()
                .one()
        }
    }

    override fun getPotAmount(roundId: Long): Int {
        return jdbi.withHandle<Int, Exception> { handle ->
            handle.createQuery(RoundSql.GET_POT_AMOUNT)
                .bind("id", roundId)
                .mapTo<Int>()
                .one()
        }
    }

    override fun allPlayersPlayed(roundId: Long): Boolean {
        return jdbi.withHandle<Boolean, Exception> { handle ->
            val count = handle.createQuery(RoundSql.COUNT_PLAYERS_PLAYED)
                .bind("round_id", roundId)
                .mapTo<Int>()
                .one()

            val totalPlayers = handle.createQuery(RoundSql.COUNT_TOTAL_PLAYERS)
                .bind("round_id", roundId)
                .mapTo<Int>()
                .one()

            count == totalPlayers && totalPlayers > 0
        }
    }

    override fun getRoundWithHands(roundId: Long): Round? {
        return jdbi.withHandle<Round?, Exception> { handle ->
            val round = handle.createQuery(RoundSql.SELECT_ROUND_BY_ID)
                .bind("id", roundId)
                .map(RoundMapper())
                .findOne()
                .orElse(null)

            if (round != null) {
                val hands = handle.createQuery(RoundSql.SELECT_HANDS_BY_ROUND)
                    .bind("round_id", roundId)
                    .map { rs, _ ->
                        val userId = rs.getInt("user_id")

                        // Obter as faces dos dados da consulta SQL
                        val faces = listOf(
                            enumValueOf<Face>(rs.getString("d1")),
                            enumValueOf<Face>(rs.getString("d2")),
                            enumValueOf<Face>(rs.getString("d3"))
                        )

                        // Criar o objeto Hand com as faces recuperadas
                        val hand = Hand(faces = faces)
                        Pair(userId, hand)
                    }
                    .list()
                    .toMap()

                return@withHandle round.copy(hands = hands)
            }


            null
        }
    }

    override fun hasPlayerPlayed(roundId: Long, userId: Int): Boolean {
        return jdbi.withHandle<Boolean, Exception> { handle ->
            val count = handle.createQuery(RoundSql.CHECK_PLAYER_PLAYED)
                .bind("round_id", roundId)
                .bind("user_id", userId)
                .mapTo<Int>()
                .one()

            count > 0
        }
    }
}*/
