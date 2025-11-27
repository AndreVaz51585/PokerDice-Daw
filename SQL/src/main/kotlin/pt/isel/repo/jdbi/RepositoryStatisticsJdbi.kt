package pt.isel.repo.jdbi

import org.jdbi.v3.core.Handle
import pt.isel.domain.Game.money.Wallet
import pt.isel.domain.user.Statistics

import pt.isel.repo.RepositoryStatistics
import pt.isel.repo.jdbi.sql.WalletSql
import pt.isel.repo.jdbi.sql.StatisticsSql


class RepositoryStatisticsJdbi (private val handle: Handle) : RepositoryStatistics {
    override fun createStatistics(userId: Int) : Statistics{
        handle.createUpdate(
            StatisticsSql.CREATE_STATISTICS,
        )
            .bind("user_id", userId)
            .execute()
        return Statistics(userId, 0,0,0,0,0,0,0,0,0,0)
    }

    override fun incrementGamesPlayed(userId: Int) {
        handle.createUpdate(
            StatisticsSql.INCREMENT_GAMES
        )
            .bind("userId", userId)
            .execute()
    }

    override fun incrementGamesWon(userId: Int) {
        handle.createUpdate(
            StatisticsSql.INCREMENT_GAMES_WON
        )
            .bind("userId", userId)
            .execute()
    }

    override fun incrementFiveOfAKind(userId: Int) {
        handle.createUpdate(
            StatisticsSql.INCREMENT_FIVE_OF_A_KIND
        )
            .bind("userId", userId)
            .execute()
    }

    override fun incrementFourOfAKind(userId: Int) {
        handle.createUpdate(
            StatisticsSql.INCREMENT_FOUR_OF_A_KIND
        )
            .bind("userId", userId)
            .execute()
    }

    override fun incrementFullHouse(userId: Int) {
        handle.createUpdate(
            StatisticsSql.INCREMENT_FULL_HOUSE
        )
            .bind("userId", userId)
            .execute()
    }

    override fun incrementStraight(userId: Int) {
        handle.createUpdate(
            StatisticsSql.INCREMENT_STRAIGHT
        )
            .bind("userId", userId)
            .execute()
    }

    override fun incrementThreeOfAKind(userId: Int) {
        handle.createUpdate(
            StatisticsSql.INCREMENT_THREE_OF_A_KIND
        )
            .bind("userId", userId)
            .execute()
    }

    override fun incrementTwoPair(userId: Int) {
        handle.createUpdate(
            StatisticsSql.INCREMENT_TWO_PAIR
        )
            .bind("userId", userId)
            .execute()
    }

    override fun incrementOnePair(userId: Int) {
        handle.createUpdate(
            StatisticsSql.INCREMENT_ONE_PAIR
        )
            .bind("userId", userId)
            .execute()
    }

    override fun incrementBust(userId: Int) {
        handle.createUpdate(
            StatisticsSql.INCREMENT_BUST
        )
            .bind("userId", userId)
            .execute()
    }

    override fun findById(id: Int): Statistics? {
        val stats = handle.createQuery(StatisticsSql.SELECT_STATISTICS)
            .bind("user_id", id)
            .map { rs, _ ->
                Statistics(
                    userId = rs.getInt("user_id"),
                    gamesPlayed = rs.getInt("games_played"),
                    gamesWon = rs.getInt("games_won"),
                    fiveOfAKind = rs.getInt("five_of_a_kind"),
                    fourOfAKind = rs.getInt("four_of_a_kind"),
                    fullHouse = rs.getInt("full_house"),
                    straight = rs.getInt("straight"),
                    threeOfAKind = rs.getInt("three_of_a_kind"),
                    twoPair = rs.getInt("two_pair"),
                    onePair = rs.getInt("one_pair"),
                    bust = rs.getInt("bust"),
                )
            }
            .findOne()
            .orElse(null)
            ?: return null

        return stats
    }

    override fun findAll(): List<Statistics> {
        val stats = handle.createQuery(StatisticsSql.SELECT_ALL_STATISTICS)
            .map { rs, _ ->
                Statistics(
                    userId = rs.getInt("user_id"),
                    gamesPlayed = rs.getInt("games_played"),
                    gamesWon = rs.getInt("games_won"),
                    fiveOfAKind = rs.getInt("five_of_a_kind"),
                    fourOfAKind = rs.getInt("four_of_a_kind"),
                    fullHouse = rs.getInt("full_house"),
                    straight = rs.getInt("straight"),
                    threeOfAKind = rs.getInt("three_of_a_kind"),
                    twoPair = rs.getInt("two_pair"),
                    onePair = rs.getInt("one_pair"),
                    bust = rs.getInt("bust"),
                )
            }
            .list()

        // N+1. Para volume elevado, considerar join + agregação.
        return stats
    }

    override fun save(entity: Statistics) {
        TODO("Not yet implemented")
    }

    override fun deleteById(id: Int): Boolean {
        TODO("Not yet implemented")
    }

    override fun clear() {
        handle.createUpdate(StatisticsSql.CLEAR_STATISTICS).execute()
    }

}