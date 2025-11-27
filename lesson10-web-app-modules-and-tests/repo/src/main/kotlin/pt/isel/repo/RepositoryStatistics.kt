package pt.isel.repo

import pt.isel.domain.Game.money.Wallet
import pt.isel.domain.user.Statistics

interface RepositoryStatistics: Repository<Statistics>  {

    fun createStatistics (userId: Int): Statistics

    fun incrementGamesPlayed (userId: Int)

    fun incrementGamesWon (userId: Int)

    fun incrementFiveOfAKind(userId: Int)

    fun incrementFourOfAKind(userId: Int)

    fun incrementFullHouse(userId: Int)

    fun incrementStraight(userId: Int)

    fun incrementThreeOfAKind(userId: Int)

    fun incrementTwoPair(userId: Int)

    fun incrementOnePair(userId: Int)

    fun incrementBust(userId: Int)
}