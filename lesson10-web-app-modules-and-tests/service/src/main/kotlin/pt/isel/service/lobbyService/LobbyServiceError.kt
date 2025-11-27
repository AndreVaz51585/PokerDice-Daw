package pt.isel.service.lobbyService

sealed class LobbyServiceError {
    data object UserNotFound : LobbyServiceError()

    data object LobbyNotFound : LobbyServiceError()

    data object LobbyClosed : LobbyServiceError()

    data object LobbyFull : LobbyServiceError()

    data object AlreadyInLobby : LobbyServiceError()

    data object ErrorJoiningLobby : LobbyServiceError()

    data object UserIsNotInLobby : LobbyServiceError()

    data object ErrorLeavingLobby : LobbyServiceError()

    data object ErrorCreatingMatch : LobbyServiceError()

    data object NotEnoughMoney : LobbyServiceError()
}