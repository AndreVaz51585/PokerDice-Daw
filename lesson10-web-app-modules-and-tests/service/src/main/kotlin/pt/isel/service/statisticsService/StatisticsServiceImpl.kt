package pt.isel.service.statisticsService

import org.springframework.stereotype.Service
import pt.isel.domain.user.Statistics
import pt.isel.repo.RepositoryUser
import pt.isel.repo.RepositoryWallet
import pt.isel.repo.TransactionManager
import pt.isel.service.Auxiliary.Either
import pt.isel.service.Auxiliary.Success
import pt.isel.service.Auxiliary.failure
import pt.isel.service.Auxiliary.success

@Service
class StatisticsServiceImpl(
    private val repoUser: RepositoryUser,
    private val trxManager: TransactionManager,
) : StatisticsService {
    override fun createStatistics(userId: Int): Either<StatisticsServiceError, Statistics> =
        trxManager.run {
            if (repoUser.findById(userId) == null) {
                return@run failure(StatisticsServiceError.UserNotFound)
            }
            val stats = repoStatistics.createStatistics(userId)

            return@run Success(stats)
        }

    override fun getStatistics(userId: Int): Either<StatisticsServiceError, Statistics> =
        trxManager.run {

            val stats = repoStatistics.findById(userId) ?: return@run failure(StatisticsServiceError.StatisticsNotFound)

            return@run Success(stats)
        }

    override fun getAll(): Either<StatisticsServiceError, List<Statistics>> =
        trxManager.run {
            val all = repoStatistics.findAll()
            success(all)
        }

    override fun incrementGamesPlayed(userId: Int): Either<StatisticsServiceError, Unit> =
        trxManager.run {

            val stats = repoStatistics.findById(userId) ?: return@run failure(StatisticsServiceError.StatisticsNotFound)

            val new = repoStatistics.incrementGamesPlayed(userId)

            success(new)
        }

    override fun incrementGamesWon(userId: Int): Either<StatisticsServiceError, Unit> =
        trxManager.run {

            val stats = repoStatistics.findById(userId) ?: return@run failure(StatisticsServiceError.StatisticsNotFound)

            val new = repoStatistics.incrementGamesWon(userId)

            success(new)
        }

    override fun incrementFiveOfAKind(userId: Int): Either<StatisticsServiceError, Unit> =
        trxManager.run {

            val stats = repoStatistics.findById(userId) ?: return@run failure(StatisticsServiceError.StatisticsNotFound)

            val new = repoStatistics.incrementFiveOfAKind(userId)

            success(new)
        }


    override fun incrementFourOfAKind(userId: Int): Either<StatisticsServiceError, Unit> =
        trxManager.run {

            val stats = repoStatistics.findById(userId) ?: return@run failure(StatisticsServiceError.StatisticsNotFound)

            val new = repoStatistics.incrementFourOfAKind(userId)

            success(new)
        }

    override fun incrementFullHouse(userId: Int): Either<StatisticsServiceError, Unit> =
        trxManager.run {

            val stats = repoStatistics.findById(userId) ?: return@run failure(StatisticsServiceError.StatisticsNotFound)

            val new = repoStatistics.incrementFullHouse(userId)

            success(new)
        }

    override fun incrementStraight(userId: Int): Either<StatisticsServiceError, Unit> =
        trxManager.run {

            val stats = repoStatistics.findById(userId) ?: return@run failure(StatisticsServiceError.StatisticsNotFound)

            val new = repoStatistics.incrementStraight(userId)

            success(new)
        }

    override fun incrementThreeOfAKind(userId: Int): Either<StatisticsServiceError, Unit> =
        trxManager.run {

            val stats = repoStatistics.findById(userId) ?: return@run failure(StatisticsServiceError.StatisticsNotFound)

            val new = repoStatistics.incrementThreeOfAKind(userId)

            success(new)
        }

    override fun incrementTwoPair(userId: Int): Either<StatisticsServiceError, Unit> =
        trxManager.run {

            val stats = repoStatistics.findById(userId) ?: return@run failure(StatisticsServiceError.StatisticsNotFound)

            val new = repoStatistics.incrementTwoPair(userId)

            success(new)
        }

    override fun incrementOnePair(userId: Int, ): Either<StatisticsServiceError, Unit> =
        trxManager.run {

            val stats = repoStatistics.findById(userId) ?: return@run failure(StatisticsServiceError.StatisticsNotFound)

            val new = repoStatistics.incrementOnePair(userId)

            success(new)
        }

    override fun incrementBust(userId: Int): Either<StatisticsServiceError, Unit> =
        trxManager.run {

            val stats = repoStatistics.findById(userId) ?: return@run failure(StatisticsServiceError.StatisticsNotFound)

            val new = repoStatistics.incrementBust(userId)

            success(new)
        }
    //TODO: To Refactor
    /*fun <T> increment (userId: Int, pathId : Int, operacao: (Int) -> T): Either<StatisticsServiceError, Unit> =
        trxManager.run {
            if (userId != pathId){
                return@run failure(StatisticsServiceError.NoPermission)
            }
            val stats = repoStatistics.findById(userId) ?: return@run failure(StatisticsServiceError.StatisticsNotFound)

            val new = operacao(userId)

            return@run success(new)
        }*/
}
