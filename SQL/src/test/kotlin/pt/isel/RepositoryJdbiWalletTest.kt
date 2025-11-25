package pt.isel

import org.jdbi.v3.core.Jdbi
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.postgresql.ds.PGSimpleDataSource
import pt.isel.domain.authentication.PasswordValidationInfo
import pt.isel.repo.jdbi.TransactionManagerJdbi
import pt.isel.repo.jdbi.configureWithAppRequirements
import pt.isel.domain.Game.money.Wallet


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

            assertEquals(loaded!!.userId, user.id)
            assertEquals(loaded.currentBalance, 0)

            val new = repoWallet.save(
                Wallet(user.id, 100)
            )

            // Test = Save & Get Balance
            assertEquals(repoWallet.findById(user.id)!!.currentBalance, 100)
            assertEquals(repoWallet.getBalance(user.id),100)

            // Test = Delete
            assertEquals(repoWallet.deleteById(user.id), true)
            assertEquals(repoWallet.findById(user.id), null)

        }
    }
}