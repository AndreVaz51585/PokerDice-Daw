package pt.isel

import org.jdbi.v3.core.Jdbi
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.postgresql.ds.PGSimpleDataSource
import pt.isel.domain.authentication.PasswordValidationInfo
import pt.isel.repo.jdbi.TransactionManagerJdbi
import pt.isel.repo.jdbi.configureWithAppRequirements
import kotlin.test.*

class RepositoryJdbiWalletTest {
    companion object {
        private val jdbi = Jdbi.create(
            PGSimpleDataSource().apply {
                val url = Environment.getDbUrl()
                setURL(url)
            },
        ).configureWithAppRequirements()
        val trxManager = TransactionManagerJdbi(jdbi)
    }

    @BeforeEach
    fun setup() {
        trxManager.run {
            // Limpar na ordem correta para não violar FKs
            repoRound.clear()
            repoMatch.clear()
            repoLobbies.clear()
            repoUsers.clear()
        }
    }

    @Test
    fun `createRound e findById`() {
        trxManager.run {
            var user = repoUsers.createUser(
                name = "João",
                email = "joao@gmail.com",
                passwordValidation = PasswordValidationInfo(validationInfo = "hashedPassword"),
                invitationCode = "11111111111111111",
            )

            var wallet = repoWallet.createWallet(
                user_id = user.id
            )


            println("Wallet criado com id = ${user.id}")

            val loaded = repoWallet.findById(user.id)
            println("Wallet created: $loaded")

            assertNotNull(loaded)

            assertEquals(loaded.userId, user.id)
            assertEquals(loaded.currentBalance, 0)
        }
    }
}