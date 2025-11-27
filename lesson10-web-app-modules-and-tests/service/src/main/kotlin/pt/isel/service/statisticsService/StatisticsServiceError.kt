package pt.isel.service.statisticsService

sealed class StatisticsServiceError {
    data object UserNotFound : StatisticsServiceError()

    data object NoPermission : StatisticsServiceError()

    data object StatisticsNotFound : StatisticsServiceError()
}
