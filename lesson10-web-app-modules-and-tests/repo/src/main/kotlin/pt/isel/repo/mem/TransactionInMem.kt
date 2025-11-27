package pt.isel.repo.mem


import pt.isel.repo.*

class TransactionInMem(
    override val repoUsers: RepositoryUser,
    override val repoLobbies: RepositoryLobby,
    override val repoMatch: RepositoryMatch,
    override val repoRound: RepositoryRound,
    override val repoWallet: RepositoryWallet,
    override val repoStatistics: RepositoryStatistics
) : Transaction {
    override fun rollback(): Unit = throw UnsupportedOperationException()
}