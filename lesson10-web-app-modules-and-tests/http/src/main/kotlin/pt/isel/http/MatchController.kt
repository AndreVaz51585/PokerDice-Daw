package pt.isel.http

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import pt.isel.domain.Game.Match.Match
import pt.isel.domain.Game.Match.MatchPlayer
import pt.isel.domain.Game.Match.MatchState
import pt.isel.http.model.MatchInput
import pt.isel.http.model.Problem
import pt.isel.service.Auxiliary.Either
import pt.isel.service.Auxiliary.Failure
import pt.isel.service.Auxiliary.Success
import pt.isel.service.matchService.MatchService
import pt.isel.service.matchService.MatchServiceError

@RestController
class MatchController(
    private val matchService: MatchService
) {
    @PostMapping("/api/matches")
    fun createMatch(
        @RequestBody input: MatchInput
    ): ResponseEntity<*> {
        val result: Either<MatchServiceError, Match> = matchService.createMatch(
            lobbyId = input.lobbyId,
            totalRounds = input.totalRounds,
            ante = input.ante,
        )

        return when (result) {
            is Success ->
                ResponseEntity
                    .status(HttpStatus.CREATED)
                    .header(
                        "Location",
                        "/api/matches/${result.value.id}",
                    ).build<Unit>()

            is Failure -> when (result.value) {
                MatchServiceError.PlayerNotFound -> Problem.UserNotFound.response(HttpStatus.NOT_FOUND)
                MatchServiceError.InvalidState -> Problem.InvalidRequest.response(HttpStatus.BAD_REQUEST)
                MatchServiceError.PlayerAlreadyInMatch -> Problem.AlreadyInMatch.response(HttpStatus.BAD_REQUEST)
                MatchServiceError.MatchFull -> Problem.MatchFull.response(HttpStatus.BAD_REQUEST)
                MatchServiceError.Unknown -> Problem.Unknown.response(HttpStatus.INTERNAL_SERVER_ERROR)
                else -> Problem.Unknown.response(HttpStatus.INTERNAL_SERVER_ERROR)
            }
        }
    }

    @GetMapping("/api/matches/{id}")
    fun getMatchById(
        @PathVariable id: Int
    ): ResponseEntity<*> {
        val result: Either<MatchServiceError, Match> = matchService.getMatch(id)

        return when (result) {
            is Success ->
                ResponseEntity
                    .status(HttpStatus.OK)
                    .body(result.value)

            is Failure -> Problem.MatchNotFound.response(HttpStatus.NOT_FOUND)
        }
    }

    @GetMapping("/api/matches")
    fun getMatches(): ResponseEntity<*> {
        // Como não há um método específico para listar todas as partidas,
        // você pode precisar implementar isso no MatchService
        return ResponseEntity
            .status(HttpStatus.OK)
            .body(emptyList<Match>()) // Implementação temporária
    }

    @GetMapping("/api/matches/{id}/players")
    fun getMatchPlayers(
        @PathVariable id: Int
    ): ResponseEntity<*> {
        val matchResult = matchService.getMatch(id)
        if (matchResult is Failure) {
            return Problem.MatchNotFound.response(HttpStatus.NOT_FOUND)
        }

        val players = matchService.listPlayers(id)
        return ResponseEntity
            .status(HttpStatus.OK)
            .body(players)
    }

    @PostMapping("/api/matches/{id}/start")
    fun startMatch(
        @PathVariable id: Int
    ): ResponseEntity<*> {
        val matchResult = matchService.getMatch(id)
        if (matchResult is Failure) {
            return Problem.MatchNotFound.response(HttpStatus.NOT_FOUND)
        }

        val result = matchService.updateState(id, MatchState.RUNNING)

        return when (result) {
            is Success -> ResponseEntity.status(HttpStatus.OK).build<Unit>()
            is Failure -> when (result.value) {
                MatchServiceError.InvalidState -> Problem.InvalidRequest.response(HttpStatus.BAD_REQUEST)
                is MatchServiceError.Unknown -> Problem.Unknown.response(HttpStatus.INTERNAL_SERVER_ERROR)
                else -> Problem.Unknown.response(HttpStatus.INTERNAL_SERVER_ERROR)
            }
        }
    }

    @PostMapping("/api/matches/{id}/end")
    fun endMatch(
        @PathVariable id: Int
    ): ResponseEntity<*> {
        val matchResult = matchService.getMatch(id)
        if (matchResult is Failure) {
            return Problem.MatchNotFound.response(HttpStatus.NOT_FOUND)
        }

        val result = matchService.updateState(id, MatchState.FINISHED)

        return when (result) {
            is Success -> ResponseEntity.status(HttpStatus.OK).build<Unit>()
            is Failure -> when (result.value) {
                MatchServiceError.InvalidState -> Problem.InvalidRequest.response(HttpStatus.BAD_REQUEST)
                is MatchServiceError.Unknown -> Problem.Unknown.response(HttpStatus.INTERNAL_SERVER_ERROR)
                else -> Problem.Unknown.response(HttpStatus.INTERNAL_SERVER_ERROR)
            }
        }
    }
}
