package pt.isel.http.controllers

import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import pt.isel.domain.Game.Lobby.Lobby
import pt.isel.domain.Game.Lobby.LobbyEvent
import pt.isel.domain.Game.Lobby.Event.lobbySnapshot
import pt.isel.domain.Game.Lobby.Event.matchStarting
import pt.isel.domain.Game.Lobby.Event.playerJoined
import pt.isel.domain.Game.Lobby.Event.playerLeft
import pt.isel.domain.Game.Lobby.responses.startedMatch
import pt.isel.domain.Game.Lobby.responses.succesfullJoin
import pt.isel.domain.Game.Lobby.responses.sucessfullLeave
import pt.isel.domain.user.AuthenticatedUser
import pt.isel.domain.user.Player
import pt.isel.http.model.lobby.LobbyInput
import pt.isel.http.model.problem.Problem
import pt.isel.service.Auxiliary.Either
import pt.isel.service.Auxiliary.Failure
import pt.isel.service.Auxiliary.Success
import pt.isel.service.lobbyService.LobbyService
import pt.isel.service.lobbyService.LobbyServiceError
import pt.isel.service.matchService.MatchService
import java.util.concurrent.TimeUnit
import kotlin.compareTo

@RestController
@RequestMapping("/api/lobbies")
class LobbyController(
    private val lobbyService: LobbyService,
) {

    @PostMapping
    fun createLobby(
        @RequestBody input: LobbyInput,
        authenticatedUser: AuthenticatedUser,
    ): ResponseEntity<*> {

        val result: Either<LobbyServiceError, Lobby> =
            lobbyService
                .createLobby(
                    authenticatedUser.user.id,
                    input.name,
                    input.description,
                    input.minPlayers,
                    input.maxPlayers,
                    input.rounds,
                    input.ante
                )


        return when (result) {

            is Success ->
                ResponseEntity
                    .status(HttpStatus.SEE_OTHER)
                    .header(
                        "Location",
                        "/api/lobbies/${result.value.id}",
                    ).build<Unit>()

            is Failure -> when (result.value) {
                LobbyServiceError.UserNotFound -> Problem.UserNotFound.response(HttpStatus.NOT_FOUND)
                LobbyServiceError.LobbyNotFound -> Problem.LobbyNotFound.response(HttpStatus.NOT_FOUND)
                LobbyServiceError.LobbyClosed -> Problem.LobbyClosed.response(HttpStatus.BAD_REQUEST)
                LobbyServiceError.LobbyFull -> Problem.LobbyFull.response(HttpStatus.BAD_REQUEST)
                LobbyServiceError.ErrorJoiningLobby -> Problem.ErrorJoiningLobby.response(HttpStatus.BAD_REQUEST)
                LobbyServiceError.AlreadyInLobby -> Problem.AlreadyInLobby.response(HttpStatus.BAD_REQUEST)
                LobbyServiceError.NotEnoughMoney -> Problem.NotEnoughMoney.response(HttpStatus.BAD_REQUEST)
                LobbyServiceError.UserAlreadyInAnotherLobby -> Problem.UserAlreadyInAnotherLobby.response((HttpStatus.BAD_REQUEST))
                else -> Problem.ErrorCreatingLobby.response(HttpStatus.BAD_REQUEST)
            }
        }

    }


    @GetMapping
    fun getAllLobbies(): ResponseEntity<*> {
        val lobbies: List<Lobby> = lobbyService.listOpenLobbies(100, 0)
        return ResponseEntity
            .status(HttpStatus.OK)
            .body(lobbies)
    }


    @GetMapping("/{id}")
    fun getLobbyById(
        @PathVariable id: Int
    ): ResponseEntity<*> {

        val result: Either<LobbyServiceError, Lobby> =
            lobbyService
                .getLobby(id)

        return when (result) {

            is Success ->
                ResponseEntity
                    .status(HttpStatus.OK)
                    .body(result.value)


            is Failure ->
                Problem.LobbyNotFound.response(HttpStatus.NOT_FOUND)

        }
    }


    @GetMapping("/{id}/events", produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun listenLobbyEvents(@PathVariable id: Int): SseEmitter {
        val emitter = SseEmitter(TimeUnit.HOURS.toMillis(1))

        // Obter snapshot inicial do lobby
        when (val lobbyResult = lobbyService.getLobby(id)) {
            is Success -> {
                val lobby = lobbyResult.value
                val players = when (val playersResult = lobbyService.listPlayers(id)) {
                    is Success -> playersResult.value.map { player -> Player(player.id,player.name) }
                    is Failure -> emptyList()
                }

                try {
                    val snapshot = LobbyEvent.LobbySnapshot(lobby, players)
                    emitter.send(
                        SseEmitter.event()
                            .name("lobby-snapshot")
                            .data(
                                lobbySnapshot(
                                    lobby = snapshot.lobby,
                                    players = snapshot.players,
                                    currentCount = snapshot.players.size
                                )
                            )
                    )
                } catch (ex: Exception) {
                    emitter.completeWithError(ex)
                    return emitter
                }
            }

            is Failure -> {
                emitter.completeWithError(IllegalArgumentException("Lobby não encontrado"))
                return emitter
            }
        }

        // Subscrever eventos futuros
        val unsubscribe = lobbyService.getEventPublisher().subscribe(id) { event ->
            try {
                when (event) {
                    is LobbyEvent.PlayerJoined -> {
                        emitter.send(
                            SseEmitter.event()
                                .name("player-joined")
                                .data(
                                    playerJoined(
                                        player = event.player,
                                        currentCount = event.currentCount,
                                        maxPlayers = event.maxPlayers
                                    )
                                )
                        )
                    }

                    is LobbyEvent.PlayerLeft -> {
                        emitter.send(
                            SseEmitter.event()
                                .name("player-left")
                                .data(
                                    playerLeft(
                                        player = event.player,
                                        currentCount = event.currentCount
                                    )
                                )
                        )
                    }

                    is LobbyEvent.MatchStarting -> {
                        emitter.send(
                            SseEmitter.event()
                                .name("match-starting")
                                .data(
                                    matchStarting(
                                        matchId = event.matchId
                                    )
                                )
                        )
                    }

                    is LobbyEvent.LobbyClosed -> {
                        emitter.send(
                            SseEmitter.event()
                                .name("lobby-closed")
                                .data(mapOf("reason" to "Host saiu"))
                        )
                        emitter.complete()
                    }

                    is LobbyEvent.TimeoutUpdate -> {
                        emitter.send(
                            SseEmitter.event()
                                .name("timeout-update")
                                .data(mapOf("remainingSeconds" to event.remainingSeconds))
                        )
                    }

                    else -> {}
                }
            } catch (ex: Exception) {
                emitter.completeWithError(ex)
            }
        }

        emitter.onCompletion { unsubscribe() }
        emitter.onTimeout { unsubscribe() }
        emitter.onError { unsubscribe() }

        return emitter
    }

    @PostMapping("/{id}/join")
    fun joinLobby(
        @PathVariable id: Int,
        authenticatedUser: AuthenticatedUser
    ): ResponseEntity<*> {
        val userId = authenticatedUser.user.id
        val result: Either<LobbyServiceError, Int> = lobbyService.joinLobby(id, userId)

        return when (result) {
            is Success -> {
                val matchId = result.value
                if (matchId > 0) {
                    // Match já começou
                    ResponseEntity.status(HttpStatus.OK).body(
                        startedMatch(
                            lobbyId = id,
                            matchId = matchId,
                            message = "Partida iniciada com sucesso"
                        )
                    )
                } else {
                    // Apenas entrou no lobby
                    ResponseEntity.status(HttpStatus.OK).body(
                        succesfullJoin(
                            lobbyId = id,
                            message = "Entrou no lobby com sucesso"
                        )
                    )
                }
            }

            is Failure -> when (result.value) {
                LobbyServiceError.LobbyNotFound ->
                    Problem.LobbyNotFound.response(HttpStatus.NOT_FOUND)

                LobbyServiceError.LobbyFull ->
                    Problem.LobbyFull.response(HttpStatus.BAD_REQUEST)

                LobbyServiceError.AlreadyInLobby ->
                    Problem.AlreadyInLobby.response(HttpStatus.BAD_REQUEST)

                LobbyServiceError.UserAlreadyInAnotherLobby ->
                    Problem.UserAlreadyInAnotherLobby.response(HttpStatus.BAD_REQUEST)

                LobbyServiceError.NotEnoughMoney ->
                    Problem.NotEnoughMoney.response(HttpStatus.BAD_REQUEST)

                else -> Problem.Unknown.response(HttpStatus.INTERNAL_SERVER_ERROR)
            }
        }
    }
    @PostMapping("/{id}/leave")
    fun leaveLobby(
        @PathVariable id: Int,
        authenticatedUser: AuthenticatedUser
    ): ResponseEntity<*> {
        val userId = authenticatedUser.user.id
        val result: Either<LobbyServiceError, Boolean> = lobbyService.leaveLobby(id, userId)

        return when (result) {
            is Success -> {
                ResponseEntity.status(HttpStatus.OK).body(
                    sucessfullLeave(
                        lobbyId = id,
                        message = "Saiu do lobby com sucesso",
                    )
                )
            }
            is Failure -> when (result.value) {
                LobbyServiceError.LobbyNotFound ->
                    Problem.LobbyNotFound.response(HttpStatus.NOT_FOUND)
                LobbyServiceError.UserIsNotInLobby ->
                    Problem.UserIsNotInLobby.response(HttpStatus.BAD_REQUEST)
                else -> Problem.Unknown.response(HttpStatus.INTERNAL_SERVER_ERROR)
            }
        }
    }


    // Listar jogadores de um lobby
    @GetMapping("/{id}/players")
    fun listPlayers(
        @PathVariable id: Int
    ): ResponseEntity<*> {
        val players = lobbyService.listPlayers(id)
        return when (players) {
            is Success -> {
                ResponseEntity.status(HttpStatus.OK).body(
                    players.value.map { player -> Player(player.id, player.name) }
                )
            }

            is Failure -> Problem.LobbyNotFound.response(HttpStatus.NOT_FOUND)
        }
    }

    @GetMapping("/{id}/host")
    fun getLobbyHost(
        @PathVariable id: Int
    ): ResponseEntity<*> {
        val lobbyResult: Either<LobbyServiceError, Lobby> = lobbyService
            .getLobby(id)

        return when (lobbyResult) {
            is Success -> {
                when (val hostResult = lobbyService.getLobbyHost(lobbyResult.value)) {
                    is Success -> ResponseEntity.status(HttpStatus.OK).body(hostResult.value)
                    is Failure -> Problem.UserNotFound.response(HttpStatus.NOT_FOUND)
                }
            }

            is Failure -> Problem.LobbyNotFound.response(HttpStatus.NOT_FOUND)
        }
    }


}

