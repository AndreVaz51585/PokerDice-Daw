package pt.isel.service.statisticsService

import pt.isel.domain.user.Statistics
import pt.isel.service.Auxiliary.Either

interface StatisticsService {
    fun createStatistics(userId: Int): Either<StatisticsServiceError, Statistics>

    fun getStatistics(userId: Int): Either<StatisticsServiceError, Statistics>

    fun getAll(): Either<StatisticsServiceError, List<Statistics>>

    fun incrementGamesPlayed(userId: Int): Either<StatisticsServiceError, Unit>

    fun incrementGamesWon(userId: Int): Either<StatisticsServiceError, Unit>

    fun incrementFiveOfAKind(userId: Int): Either<StatisticsServiceError, Unit>

    fun incrementFourOfAKind(userId: Int): Either<StatisticsServiceError, Unit>

    fun incrementFullHouse(userId: Int): Either<StatisticsServiceError, Unit>

    fun incrementStraight(userId: Int): Either<StatisticsServiceError, Unit>

    fun incrementThreeOfAKind(userId: Int): Either<StatisticsServiceError, Unit>

    fun incrementTwoPair(userId: Int): Either<StatisticsServiceError, Unit>

    fun incrementOnePair(userId: Int): Either<StatisticsServiceError, Unit>

    fun incrementBust(userId: Int): Either<StatisticsServiceError, Unit>
}
