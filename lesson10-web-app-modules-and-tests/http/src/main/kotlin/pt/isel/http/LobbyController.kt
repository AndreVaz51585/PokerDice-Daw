package pt.isel.http

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import pt.isel.domain.Game.Lobby.Lobby
import pt.isel.domain.user.AuthenticatedUser
import pt.isel.http.model.LobbyInput
import pt.isel.http.model.Problem
import pt.isel.service.Auxiliary.Either
import pt.isel.service.Auxiliary.Failure
import pt.isel.service.Auxiliary.Success
import pt.isel.service.lobbyService.LobbyService
import pt.isel.service.lobbyService.LobbyServiceError

@RestController
class LobbyController(
    private val lobbyService: LobbyService
) {

    @PostMapping("/api/lobbies")
    fun createLobby(
        @RequestBody input: LobbyInput
    ): ResponseEntity<*> {

        val result: Either<LobbyServiceError, Lobby> = lobbyService.createLobby(
            input.lobbyHostId,
            input.name,
            input.description,
            input.minPlayers,
            input.maxPlayers,
            input.rounds,
            input.ante
        )


        return when (result) {

            is Success -> ResponseEntity.status(HttpStatus.CREATED).header(
                "Location",
                "/api/lobbies/${result.value.id}",
            ).build<Unit>()

            is Failure -> Problem.UserNotFound.response(HttpStatus.NOT_FOUND)


        }

    }


    @GetMapping("/api/lobbies")
    fun getAllLobbies(): ResponseEntity<*> {
        val lobbies: List<Lobby> = lobbyService.listOpenLobbies(100, 0)
        return ResponseEntity.status(HttpStatus.OK).body(lobbies)
    }


    @GetMapping("/api/lobbies/{id}")
    fun getLobbyById(
        @PathVariable id: Int
    ): ResponseEntity<*> {

        val result: Either<LobbyServiceError, Lobby> = lobbyService.getLobby(id)

        return when (result) {

            is Success -> ResponseEntity.status(HttpStatus.OK).body(result.value)


            is Failure -> Problem.LobbyNotFound.response(HttpStatus.NOT_FOUND)

        }
    }

    @PostMapping("/api/lobbies/{id}/join")
    fun joinLobby(
        @PathVariable id: Int, @RequestBody user: AuthenticatedUser
    ): ResponseEntity<*> {
        val userId = user.user.id
        val result: Either<LobbyServiceError, Boolean> = lobbyService.joinLobby(id, userId)
        return when (result) {
            is Success -> ResponseEntity.status(HttpStatus.OK).body(true)

            is Failure -> when (result.value) {
                LobbyServiceError.UserNotFound -> Problem.UserNotFound.response(HttpStatus.NOT_FOUND)
                LobbyServiceError.LobbyNotFound -> Problem.LobbyNotFound.response(HttpStatus.NOT_FOUND)
                LobbyServiceError.LobbyClosed -> Problem.LobbyClosed.response(HttpStatus.BAD_REQUEST)
                LobbyServiceError.LobbyFull -> Problem.LobbyFull.response(HttpStatus.BAD_REQUEST)
                LobbyServiceError.ErrorJoiningLobby -> Problem.ErrorJoiningLobby.response(HttpStatus.BAD_REQUEST)
                LobbyServiceError.AlreadyInLobby -> Problem.AlreadyInLobby.response(HttpStatus.BAD_REQUEST)
            }
        }
    }

    // Remover jogador do lobby FALTA ACABAR , SE O UTILIZADOR NÂO ESTIVER NO LOBBY DEVE DAR ERRO INDICANDO QUE JÀ NÂO ESTÁ NO LOBBY
    @PostMapping("/api/lobbies/{id}/leave")
    fun leaveLobby(
        @PathVariable id: Int, @RequestBody input: AuthenticatedUser
    ): ResponseEntity<*> {
        val userId = input.user.id
        lobbyService.leaveLobby(id, userId)
        return ResponseEntity.status(HttpStatus.OK).build<Unit>()
    }

    // Listar jogadores de um lobby
    @GetMapping("/api/lobbies/{id}/players")
    fun listPlayers(
        @PathVariable id: Int
    ): ResponseEntity<*> {
        val players = lobbyService.listPlayers(id)
        return ResponseEntity.status(HttpStatus.OK).body(players)
    }

    // Obter host de um lobby
    @GetMapping("/api/lobbies/{id}/host")
    fun getLobbyHost(
        @PathVariable id: Int
    ): ResponseEntity<*> {
        val lobbyResult: Either<LobbyServiceError, Lobby> = lobbyService.getLobby(id)

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

