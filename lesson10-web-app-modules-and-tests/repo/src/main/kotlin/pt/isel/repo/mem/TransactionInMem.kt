package pt.isel.repo.mem

import pt.isel.repo.RepositoryLobby
import pt.isel.repo.RepositoryMatch
import pt.isel.repo.RepositoryRound
import pt.isel.repo.RepositoryStatistics
import pt.isel.repo.RepositoryUser
import pt.isel.repo.RepositoryWallet
import pt.isel.repo.Transaction

class TransactionInMem(
    override val repoUsers: RepositoryUser,
    override val repoLobbies: RepositoryLobby,
    override val repoMatch: RepositoryMatch,
    override val repoRound: RepositoryRound,
    override val repoWallet: RepositoryWallet,
    override val repoStatistics: RepositoryStatistics,
) : Transaction {
    override fun rollback(): Unit = throw UnsupportedOperationException()
}
