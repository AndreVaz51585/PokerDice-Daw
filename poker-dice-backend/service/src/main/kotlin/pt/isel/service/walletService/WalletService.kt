package pt.isel.service.walletService

import pt.isel.domain.Game.money.Wallet
import pt.isel.service.Auxiliary.Either

interface WalletService {
    fun createWallet(userId: Int): Either<WalletServiceError, Wallet>

    fun getWallet(userId: Int): Either<WalletServiceError, Wallet>

    fun getAmount(
        userId: Int,
        pathId: Int,
    ): Either<WalletServiceError, Int>

    fun update(wallet: Wallet): Either<WalletServiceError, Unit>

    fun getAll(): Either<WalletServiceError, List<Wallet>>

    fun deposit(
        userId: Int,
        pathId: Int,
        amount: Int,
    ): Either<WalletServiceError, Wallet>

    fun withdraw(
        userId: Int,
        pathId: Int,
        amount: Int,
    ): Either<WalletServiceError, Wallet>
}
