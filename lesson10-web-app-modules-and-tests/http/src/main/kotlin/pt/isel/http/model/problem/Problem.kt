package pt.isel.http.model.problem

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
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

    data object InvalidCredentials : Problem(URI("$PROBLEM_URI_PATH/invalid-credentials"))

    data object InsecurePassword : Problem(URI("$PROBLEM_URI_PATH/insecure-password"))

    data object UserNotFound : Problem(URI( "$PROBLEM_URI_PATH/user-not-found"))

    data object LobbyNotFound : Problem(URI("$PROBLEM_URI_PATH/lobby-not-found"))

    data object LobbyClosed : Problem(URI("$PROBLEM_URI_PATH/lobby-closed"))

    data object LobbyFull : Problem(URI("$PROBLEM_URI_PATH/lobby-full"))

    data object AlreadyInLobby : Problem(URI("$PROBLEM_URI_PATH/already-in-lobby"))

    data object ErrorJoiningLobby : Problem(URI("$PROBLEM_URI_PATH/error-joining-lobby"))

    data object ErrorCreatingLobby : Problem(URI("$PROBLEM_URI_PATH/error-creating-lobby"))

    data object ErrorLeavingLobby : Problem(URI("$PROBLEM_URI_PATH/error-leaving-lobby"))

    data object UserIsNotInLobby : Problem(URI("$PROBLEM_URI_PATH/user-is-not-in-lobby"))

    data object InvalidRequest : Problem(URI("$PROBLEM_URI_PATH/invalid-request"))

    data object AlreadyInMatch : Problem(URI("$PROBLEM_URI_PATH/already-in-match"))

    data object MatchNotFound : Problem(URI("$PROBLEM_URI_PATH/match-not-found"))

    data object Unknown : Problem(URI("$PROBLEM_URI_PATH/Unknown"))

    data object MatchFull : Problem(URI("$PROBLEM_URI_PATH/match-full"))

    data object ErrorCreatingUser : Problem(URI("$PROBLEM_URI_PATH/error-creating-user"))

    data object ErrorDeletingUser : Problem(URI("$PROBLEM_URI_PATH/error-deleting-user"))

    data object CommandUnknown : Problem(URI("$PROBLEM_URI_PATH/command-unknown"))

    data object NotYourTurn : Problem(URI("$PROBLEM_URI_PATH/not-your-turn"))

    data object InvalidBodyParameters : Problem(URI("$PROBLEM_URI_PATH/invalid-body-parameters"))

    data object InvalidIndices : Problem(URI("$PROBLEM_URI_PATH/invalid-indices"))

    data object InvitationCodeRequired : Problem(URI("$PROBLEM_URI_PATH/invitation-code-required"))

    data object InvalidInvitationCode : Problem(URI("$PROBLEM_URI_PATH/invalid-invitation-code"))

    data object InvitationCodeAlreadyUsed : Problem(URI("$PROBLEM_URI_PATH/invitation-code-already-used"))

    data object NoPermission : Problem(URI("$PROBLEM_URI_PATH/no-permission"))

    data object WalletNotFound : Problem(URI("$PROBLEM_URI_PATH/wallet-not-found"))

    data object NotEnoughMoney : Problem(URI("$PROBLEM_URI_PATH/not-enough-money"))

    data object StatisticsNotFound : Problem(URI("$PROBLEM_URI_PATH/statistics-not-found"))

    data object InvalidAmount : Problem(URI("$PROBLEM_URI_PATH/invalid-amount"))

    data object UserAlreadyInAnotherLobby : Problem(URI("$PROBLEM_URI_PATH/user-already-in-another-lobby"))
}
