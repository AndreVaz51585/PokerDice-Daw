package pt.isel.repo.jdbi

import org.jdbi.v3.core.Handle
import pt.isel.repo.RepositoryMatch
import pt.isel.repo.Transaction

class TransactionJdbi(
    private val handle: Handle,
) : Transaction {
    override val repoUsers = RepositoryUserJdbi(handle)
    override val repoLobbies = RepositoryLobbyJdbi(handle)
    override val repoMatch = RepositoryMatchJdbi(handle, repoLobbies)


    override fun rollback() {
        handle.rollback()
    }
}
