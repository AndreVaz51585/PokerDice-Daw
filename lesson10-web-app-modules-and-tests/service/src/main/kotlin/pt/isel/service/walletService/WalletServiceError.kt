package pt.isel.service.walletService

sealed class WalletServiceError {
    data object UserNotFound : WalletServiceError()

    data object WalletNotFound : WalletServiceError()

    data object NoPermission : WalletServiceError()

    data object InvalidAmount : WalletServiceError()

    data object UserInActiveLobby : WalletServiceError()
}
