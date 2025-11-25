package pt.isel.service.walletService

import org.springframework.stereotype.Service
import pt.isel.domain.Game.money.Wallet
import pt.isel.repo.RepositoryLobby
import pt.isel.repo.RepositoryUser
import pt.isel.repo.RepositoryWallet
import pt.isel.repo.TransactionManager
import pt.isel.service.Auxiliary.Either
import pt.isel.service.Auxiliary.failure
import pt.isel.service.Auxiliary.success
import pt.isel.service.lobbyService.LobbyService
import pt.isel.service.lobbyService.LobbyServiceError
import pt.isel.service.matchService.MatchManager

@Service
class WalletServiceImpl (
    private val repoUser: RepositoryUser,
    private val repoWallet: RepositoryWallet,
    private val trxManager: TransactionManager,
): WalletService {

    override fun createWallet(userId: Int): Either<WalletServiceError, Wallet> =
        trxManager.run {

            if (repoUser.findById(userId) == null) {
                return@run failure(WalletServiceError.UserNotFound)
            }

            val wallet = repoWallet.createWallet(userId)

            return@run success(wallet)
        }

    override fun getWallet(userId: Int): Either<WalletServiceError, Wallet> =
        trxManager.run {

            val wallet = repoWallet.findById(userId) ?: return@run failure(WalletServiceError.WalletNotFound)
            return@run success(wallet)
        }

    override fun getAmount(userId: Int): Either<WalletServiceError, Int> =
        trxManager.run {
            val amount = repoWallet.getBalance(userId)?: return@run failure(WalletServiceError.WalletNotFound)
            return@run success(amount)
        }

    override fun update(wallet: Wallet): Either<WalletServiceError, Unit> =
        trxManager.run {
            val new = repoWallet.save(wallet)
            return@run success(new)
        }

    override fun getAll(): Either<WalletServiceError, List<Wallet>> =
        trxManager.run {
            val all = repoWallet.findAll()
            return@run success(all)
        }
}
