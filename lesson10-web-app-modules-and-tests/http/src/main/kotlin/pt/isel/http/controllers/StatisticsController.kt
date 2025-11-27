package pt.isel.http.controllers

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController
import pt.isel.domain.user.AuthenticatedUser
import pt.isel.http.model.problem.Problem
import pt.isel.service.Auxiliary.Failure
import pt.isel.service.Auxiliary.Success
import pt.isel.service.statisticsService.StatisticsService
import pt.isel.service.statisticsService.StatisticsServiceError


@RestController
class StatisticsController(
    private val statisticsService: StatisticsService,
) {
    @GetMapping("/api/statistics")
    fun listAllStatistics(): ResponseEntity<Any> {
        return when (val result = statisticsService.getAll()) {

            is Success -> {
                ResponseEntity.ok(result.value)   // value é List<Wallet>
            }

            else -> {
                Problem.Unknown.response(
                    HttpStatus.BAD_REQUEST,
                )
            }
        }
    }

    @GetMapping("/api/statistics/{userId}")
    fun getStatistics(
        @PathVariable userId: Int
    ): ResponseEntity<Any> {

        val result = statisticsService.getStatistics(userId)

        return when (result) {

            is Success -> {
                ResponseEntity.ok(result.value)   // value é a wallet do User
            }

            is Failure -> when (result.value) {
                is StatisticsServiceError.NoPermission -> Problem.NoPermission.response(
                    HttpStatus.BAD_REQUEST,
                )

                StatisticsServiceError.UserNotFound -> Problem.UserNotFound.response(
                    HttpStatus.BAD_REQUEST,
                )

                StatisticsServiceError.StatisticsNotFound -> Problem.StatisticsNotFound.response(
                    HttpStatus.BAD_REQUEST,
                )

            }
        }
    }

    @GetMapping("/api/statistics/{userId}/inc_games_played")
    fun incrementGamesPlayed(
        @PathVariable userId: Int
    ): ResponseEntity<Any> {

        val result = statisticsService.incrementGamesPlayed(userId)

        return when (result) {

            is Success -> {
                ResponseEntity.ok(result.value)   // value é a wallet do User
            }

            is Failure -> when (result.value) {
                is StatisticsServiceError.NoPermission -> Problem.NoPermission.response(
                    HttpStatus.BAD_REQUEST,
                )

                StatisticsServiceError.UserNotFound -> Problem.UserNotFound.response(
                    HttpStatus.BAD_REQUEST,
                )

                StatisticsServiceError.StatisticsNotFound -> Problem.StatisticsNotFound.response(
                    HttpStatus.BAD_REQUEST,
                )

            }
        }
    }

    @GetMapping("/api/statistics/{userId}/inc_games_won")
    fun incrementGamesWon(
        @PathVariable userId: Int
    ): ResponseEntity<Any> {

        val result = statisticsService.incrementGamesWon(userId)

        return when (result) {

            is Success -> {
                ResponseEntity.ok(result.value)   // value é a wallet do User
            }

            is Failure -> when (result.value) {
                is StatisticsServiceError.NoPermission -> Problem.NoPermission.response(
                    HttpStatus.BAD_REQUEST,
                )

                StatisticsServiceError.UserNotFound -> Problem.UserNotFound.response(
                    HttpStatus.BAD_REQUEST,
                )

                StatisticsServiceError.StatisticsNotFound -> Problem.StatisticsNotFound.response(
                    HttpStatus.BAD_REQUEST,
                )

            }
        }
    }

    @GetMapping("/api/statistics/{userId}/inc_five_of_akind")
    fun incrementFiveOfAKind(
        @PathVariable userId: Int
    ): ResponseEntity<Any> {

        val result = statisticsService.incrementFiveOfAKind(userId)

        return when (result) {

            is Success -> {
                ResponseEntity.ok(result.value)   // value é a wallet do User
            }

            is Failure -> when (result.value) {
                is StatisticsServiceError.NoPermission -> Problem.NoPermission.response(
                    HttpStatus.BAD_REQUEST,
                )

                StatisticsServiceError.UserNotFound -> Problem.UserNotFound.response(
                    HttpStatus.BAD_REQUEST,
                )

                StatisticsServiceError.StatisticsNotFound -> Problem.StatisticsNotFound.response(
                    HttpStatus.BAD_REQUEST,
                )

            }
        }
    }

    @GetMapping("/api/statistics/{userId}/inc_four_of_akind")
    fun incrementFourOfAKind(
        @PathVariable userId: Int
    ): ResponseEntity<Any> {

        val result = statisticsService.incrementFourOfAKind(userId)

        return when (result) {

            is Success -> {
                ResponseEntity.ok(result.value)   // value é a wallet do User
            }

            is Failure -> when (result.value) {
                is StatisticsServiceError.NoPermission -> Problem.NoPermission.response(
                    HttpStatus.BAD_REQUEST,
                )

                StatisticsServiceError.UserNotFound -> Problem.UserNotFound.response(
                    HttpStatus.BAD_REQUEST,
                )

                StatisticsServiceError.StatisticsNotFound -> Problem.StatisticsNotFound.response(
                    HttpStatus.BAD_REQUEST,
                )

            }
        }
    }

    @GetMapping("/api/statistics/{userId}/inc_full_house")
    fun incrementFullHouse(
        @PathVariable userId: Int
    ): ResponseEntity<Any> {

        val result = statisticsService.incrementFullHouse(userId)

        return when (result) {

            is Success -> {
                ResponseEntity.ok(result.value)   // value é a wallet do User
            }

            is Failure -> when (result.value) {
                is StatisticsServiceError.NoPermission -> Problem.NoPermission.response(
                    HttpStatus.BAD_REQUEST,
                )

                StatisticsServiceError.UserNotFound -> Problem.UserNotFound.response(
                    HttpStatus.BAD_REQUEST,
                )

                StatisticsServiceError.StatisticsNotFound -> Problem.StatisticsNotFound.response(
                    HttpStatus.BAD_REQUEST,
                )

            }
        }
    }

    @GetMapping("/api/statistics/{userId}/inc_straight")
    fun incrementStraight(
        @PathVariable userId: Int
    ): ResponseEntity<Any> {

        val result = statisticsService.incrementStraight(userId)

        return when (result) {

            is Success -> {
                ResponseEntity.ok(result.value)   // value é a wallet do User
            }

            is Failure -> when (result.value) {
                is StatisticsServiceError.NoPermission -> Problem.NoPermission.response(
                    HttpStatus.BAD_REQUEST,
                )

                StatisticsServiceError.UserNotFound -> Problem.UserNotFound.response(
                    HttpStatus.BAD_REQUEST,
                )

                StatisticsServiceError.StatisticsNotFound -> Problem.StatisticsNotFound.response(
                    HttpStatus.BAD_REQUEST,
                )

            }
        }
    }

    @GetMapping("/api/statistics/{userId}/inc_three_of_akind")
    fun incrementThreeOfAKind(
        @PathVariable userId: Int
    ): ResponseEntity<Any> {

        val result = statisticsService.incrementThreeOfAKind(userId)

        return when (result) {

            is Success -> {
                ResponseEntity.ok(result.value)   // value é a wallet do User
            }

            is Failure -> when (result.value) {
                is StatisticsServiceError.NoPermission -> Problem.NoPermission.response(
                    HttpStatus.BAD_REQUEST,
                )

                StatisticsServiceError.UserNotFound -> Problem.UserNotFound.response(
                    HttpStatus.BAD_REQUEST,
                )

                StatisticsServiceError.StatisticsNotFound -> Problem.StatisticsNotFound.response(
                    HttpStatus.BAD_REQUEST,
                )

            }
        }
    }

    @GetMapping("/api/statistics/{userId}/inc_two_pair")
    fun incrementTwoPair(
        @PathVariable userId: Int
    ): ResponseEntity<Any> {

        val result = statisticsService.incrementTwoPair(userId)

        return when (result) {

            is Success -> {
                ResponseEntity.ok(result.value)   // value é a wallet do User
            }

            is Failure -> when (result.value) {
                is StatisticsServiceError.NoPermission -> Problem.NoPermission.response(
                    HttpStatus.BAD_REQUEST,
                )

                StatisticsServiceError.UserNotFound -> Problem.UserNotFound.response(
                    HttpStatus.BAD_REQUEST,
                )

                StatisticsServiceError.StatisticsNotFound -> Problem.StatisticsNotFound.response(
                    HttpStatus.BAD_REQUEST,
                )

            }
        }
    }

    @GetMapping("/api/statistics/{userId}/inc_one_pair")
    fun incrementOnePair(
        @PathVariable userId: Int
    ): ResponseEntity<Any> {

        val result = statisticsService.incrementOnePair(userId)

        return when (result) {

            is Success -> {
                ResponseEntity.ok(result.value)   // value é a wallet do User
            }

            is Failure -> when (result.value) {
                is StatisticsServiceError.NoPermission -> Problem.NoPermission.response(
                    HttpStatus.BAD_REQUEST,
                )

                StatisticsServiceError.UserNotFound -> Problem.UserNotFound.response(
                    HttpStatus.BAD_REQUEST,
                )

                StatisticsServiceError.StatisticsNotFound -> Problem.StatisticsNotFound.response(
                    HttpStatus.BAD_REQUEST,
                )

            }
        }
    }

    @GetMapping("/api/statistics/{userId}/inc_bust")
    fun incrementBust(
        @PathVariable userId: Int
    ): ResponseEntity<Any> {

        val result = statisticsService.incrementBust(userId)

        return when (result) {

            is Success -> {
                ResponseEntity.ok(result.value)   // value é a wallet do User
            }

            is Failure -> when (result.value) {
                is StatisticsServiceError.NoPermission -> Problem.NoPermission.response(
                    HttpStatus.BAD_REQUEST,
                )

                StatisticsServiceError.UserNotFound -> Problem.UserNotFound.response(
                    HttpStatus.BAD_REQUEST,
                )

                StatisticsServiceError.StatisticsNotFound -> Problem.StatisticsNotFound.response(
                    HttpStatus.BAD_REQUEST,
                )

            }
        }
    }

    //TODO: To Refactor
}