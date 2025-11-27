package pt.isel.http.controllers

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import pt.isel.domain.Game.Lobby.Lobby
import pt.isel.domain.user.AuthenticatedUser
import pt.isel.http.model.lobby.LobbyInput
import pt.isel.http.model.problem.Problem
import pt.isel.service.Auxiliary.Either
import pt.isel.service.Auxiliary.Failure
import pt.isel.service.Auxiliary.Success
import pt.isel.service.lobbyService.LobbyService
import pt.isel.service.lobbyService.LobbyServiceError
import pt.isel.service.matchService.MatchService

@RestController
class LobbyController(
    private val lobbyService: LobbyService,
    private val matchService: MatchService
) {

    @PostMapping("/api/lobbies")
    fun createLobby(
        @RequestBody input: LobbyInput,
        authenticatedUser : AuthenticatedUser,
    ) : ResponseEntity<*> {

        val result : Either<LobbyServiceError, Lobby> =
            lobbyService
                .createLobby(authenticatedUser.user.id, input.name, input.description, input.minPlayers, input.maxPlayers, input.rounds, input.ante)


        return when(result){

         is Success ->
                ResponseEntity
                    .status(HttpStatus.SEE_OTHER)
                    .header(
                        "Location",
                        "/api/lobbies/${result.value.id}",
                    ).build<Unit>()

         is Failure ->
             Problem.UserNotFound.response(HttpStatus.NOT_FOUND)


        }

    }


    @GetMapping("/api/lobbies")
    fun getAllLobbies(): ResponseEntity<*> {
        val lobbies: List<Lobby> = lobbyService.listOpenLobbies(100,0)
        return ResponseEntity
            .status(HttpStatus.OK)
            .body(lobbies)
    }


    @GetMapping("/api/lobbies/{id}")
    fun getLobbyById(
        @PathVariable id: Int
    ): ResponseEntity<*> {

    val result : Either<LobbyServiceError, Lobby> =
        lobbyService
            .getLobby(id)

    return when(result){

       is Success ->
            ResponseEntity
                .status(HttpStatus.OK)
                .body(result.value)


      is Failure ->
          Problem.LobbyNotFound.response(HttpStatus.NOT_FOUND)

      }
    }

    @PostMapping("/api/lobbies/{id}/join")
    fun joinLobby(
        @PathVariable id: Int,
        authenticateUser: AuthenticatedUser
    ): ResponseEntity<*> {
        val userId = authenticateUser.user.id
        val result : Either<LobbyServiceError, Int> = lobbyService
            .joinLobby(id, userId)
        return when (result) {
            is Success -> when(result.value) {
                0 -> ResponseEntity.status(HttpStatus.OK).body("Player Added to Lobby") // entrou no lobby mas o lobby não começou
                else -> {
                    val matchId = result.value
                    // Registar engine / state inicial fora da transacção para começar a emitir SSE
                    matchService.registerBankedMatchFromDb(matchId)
                    ResponseEntity.status(HttpStatus.CREATED) // aqui não está a fazer o redirect como é suposto porque o status não pode ser da familia 200 mas sim 300 portanto podemos apenas enviar o id da partida....
                        .header("Location", "/api/matches/$matchId")
                        .body(mapOf("matchId" to matchId))
                } // entrou no lobby e o lobby começou
            }
            is Failure -> when (result.value) {
                LobbyServiceError.UserNotFound -> Problem.UserNotFound.response(HttpStatus.NOT_FOUND)
                LobbyServiceError.LobbyNotFound -> Problem.LobbyNotFound.response(HttpStatus.NOT_FOUND)
                LobbyServiceError.LobbyClosed -> Problem.LobbyClosed.response(HttpStatus.BAD_REQUEST)
                LobbyServiceError.LobbyFull -> Problem.LobbyFull.response(HttpStatus.BAD_REQUEST)
                LobbyServiceError.ErrorJoiningLobby -> Problem.ErrorJoiningLobby.response(HttpStatus.BAD_REQUEST)
                LobbyServiceError.AlreadyInLobby -> Problem.AlreadyInLobby.response(HttpStatus.BAD_REQUEST)
                LobbyServiceError.NotEnoughMoney -> Problem.NotEnoughMoney.response(HttpStatus.BAD_REQUEST)
                else -> Problem.ErrorJoiningLobby.response(HttpStatus.BAD_REQUEST)
            }
        }
    }

    @PostMapping("/api/lobbies/{id}/leave")
    fun leaveLobby(
        @PathVariable id: Int,
        authenticateUser: AuthenticatedUser
    ): ResponseEntity<*> {
        val userId = authenticateUser.user.id
        val left : Either<LobbyServiceError, Boolean>  =  lobbyService.leaveLobby(id, userId)

        return when(left) {
            is Success ->
                ResponseEntity.status(HttpStatus.OK).build<Unit>()


            is Failure -> when (left.value) {
                LobbyServiceError.LobbyNotFound -> Problem.LobbyNotFound.response(HttpStatus.NOT_FOUND)
                LobbyServiceError.UserIsNotInLobby -> Problem.UserIsNotInLobby.response(HttpStatus.NOT_FOUND)
               LobbyServiceError.ErrorLeavingLobby -> Problem.ErrorJoiningLobby.response(HttpStatus.BAD_REQUEST)

                else -> Problem.ErrorLeavingLobby.response(HttpStatus.BAD_REQUEST)
            }

        }
    }

    // Listar jogadores de um lobby
    @GetMapping("/api/lobbies/{id}/players")
    fun listPlayers(
        @PathVariable id: Int
    ): ResponseEntity<*> {
        val players = lobbyService.listPlayers(id)
        return ResponseEntity.status(HttpStatus.OK).body(players)
    }

    @GetMapping("/api/lobbies/{id}/host")
    fun getLobbyHost(
        @PathVariable id: Int
    ): ResponseEntity<*> {
        val lobbyResult : Either<LobbyServiceError, Lobby> = lobbyService
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

