package pt.isel.repo.jdbi

import org.jdbi.v3.core.Handle
import pt.isel.repo.Transaction

class TransactionJdbi(
    private val handle: Handle,
) : Transaction {
    override val repoUsers = RepositoryUserJdbi(handle)
    override val repoLobbies = RepositoryLobbyJdbi(handle)


    override fun rollback() {
        handle.rollback()
    }
}
