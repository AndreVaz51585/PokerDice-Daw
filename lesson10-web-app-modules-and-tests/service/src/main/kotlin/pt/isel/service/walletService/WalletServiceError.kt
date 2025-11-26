package pt.isel.service.walletService

import pt.isel.service.lobbyService.LobbyServiceError


sealed class WalletServiceError {
    data object UserNotFound : WalletServiceError()

    data object WalletNotFound : WalletServiceError()

    data object NoPermission : WalletServiceError()

}