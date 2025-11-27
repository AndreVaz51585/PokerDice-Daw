package pt.isel.service.walletService

import org.springframework.stereotype.Service
import pt.isel.domain.Game.money.Wallet
import pt.isel.repo.RepositoryUser
import pt.isel.repo.TransactionManager
import pt.isel.service.Auxiliary.Either
import pt.isel.service.Auxiliary.failure
import pt.isel.service.Auxiliary.success


@Service
class WalletServiceImpl (
    private val repoUser: RepositoryUser,
    private val trxManager: TransactionManager,
): WalletService {

    override fun createWallet(userId: Int): Either<WalletServiceError, Wallet> =
        trxManager.run {

            if (repoUser.findById(userId) == null) {
                return@run failure(WalletServiceError.UserNotFound)
            }

            val wallet = repoWallet.createWallet(userId)

            success(wallet)
        }

    override fun getWallet(userId: Int, pathId: Int): Either<WalletServiceError, Wallet> =
        trxManager.run {

            if (userId != pathId) {
                return@run failure(WalletServiceError.NoPermission)
            }

            val wallet = repoWallet.findById(userId) ?: return@run failure(WalletServiceError.WalletNotFound)
            success(wallet)
        }

    override fun getAmount(userId: Int): Either<WalletServiceError, Int> =
        trxManager.run {
            val amount = repoWallet.getBalance(userId)?: return@run failure(WalletServiceError.WalletNotFound)
            success(amount)
        }

    override fun update(wallet: Wallet): Either<WalletServiceError, Unit> =
        trxManager.run {
            val new = repoWallet.save(wallet)
            success(new)
        }

    override fun getAll(): Either<WalletServiceError, List<Wallet>> =
        trxManager.run {
            val all = repoWallet.findAll()
            success(all)
        }

    override fun deposit(userId: Int, pathId : Int, amount: Int): Either<WalletServiceError, Wallet> =
        trxManager.run {

            if (userId != pathId) {
                return@run failure(WalletServiceError.NoPermission)
            }

            val wallet = repoWallet.findById(userId)
                ?: return@run failure(WalletServiceError.WalletNotFound)

            val updated = wallet.deposit(amount)   // chama o domínio

            repoWallet.save(updated)               // persiste

            success(updated)
        }

    override fun withdraw(userId: Int, pathId: Int, amount: Int): Either<WalletServiceError, Wallet> =
        trxManager.run {

            if (userId != pathId) {
                return@run failure(WalletServiceError.NoPermission)
            }

            val user = repoUser.findById(userId)
                ?: return@run failure(WalletServiceError.UserNotFound)
            if (user.id != userId) {
                return@run failure(WalletServiceError.UserNotFound)
            }

            val wallet = repoWallet.findById(userId)
                ?: return@run failure(WalletServiceError.WalletNotFound)

            val updated = wallet.withdraw(amount)   // domínio
            repoWallet.save(updated)                // grava na BD

            success(updated)
        }

}
