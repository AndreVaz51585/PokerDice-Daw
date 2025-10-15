package pt.isel.repo.mem


import pt.isel.repo.RepositoryLobby
import pt.isel.repo.RepositoryMatch
import pt.isel.repo.RepositoryUser
import pt.isel.repo.Transaction

class TransactionInMem(
    override val repoUsers: RepositoryUser,
    override val repoLobbies: RepositoryLobby,
    override val repoMatch: RepositoryMatch,
) : Transaction {
    override fun rollback(): Unit = throw UnsupportedOperationException()
}