package pt.isel.http.model

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import pt.isel.service.lobbyService.LobbyServiceError
import java.net.URI
import kotlin.collections.last
import kotlin.text.split

private const val MEDIA_TYPE = "application/problem+json"
private const val PROBLEM_URI_PATH =
    "https://github.com/isel-leic-daw/2025-daw-leic52d-2025-leic52d-05/tree/main/docs/Problems"

sealed class Problem(
    typeUri: URI,
) {
    @Suppress("unused")
    val type = typeUri.toString()
    val title = typeUri.toString().split("/").last()

    fun response(status: HttpStatus): ResponseEntity<Any> =
        ResponseEntity
            .status(status)
            .header("Content-Type", MEDIA_TYPE)
            .body(this)

    data object EmailAlreadyInUse : Problem(URI("$PROBLEM_URI_PATH/email-already-in-use"))

    data object InsecurePassword : Problem(URI("$PROBLEM_URI_PATH/insecure-password"))

    data object UserNotFound : Problem(URI( "$PROBLEM_URI_PATH/user-not-found"))

    data object LobbyNotFound : Problem(URI("$PROBLEM_URI_PATH/lobby-not-found"))

    data object LobbyClosed : Problem(URI("$PROBLEM_URI_PATH/lobby-closed"))

    data object LobbyFull : Problem(URI("$PROBLEM_URI_PATH/lobby-full"))

    data object AlreadyInLobby : Problem(URI("$PROBLEM_URI_PATH/already-in-lobby"))

    data object ErrorJoiningLobby : Problem(URI("$PROBLEM_URI_PATH/error-joining-lobby"))

    data object MatchNotFound : Problem(URI("$PROBLEM_URI_PATH/match-not-found"))

    data object AlreadyInMatch : Problem(URI("$PROBLEM_URI_PATH/already-in-match"))

    data object MatchFull : Problem(URI("$PROBLEM_URI_PATH/match-full"))

    data object InvalidRequest : Problem(URI("$PROBLEM_URI_PATH/invalid-request"))

    data class Unknown(val detail: String) : Problem(URI("$PROBLEM_URI_PATH/custom-problem"))
}
