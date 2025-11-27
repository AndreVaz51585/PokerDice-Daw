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
import pt.isel.domain.user.Statistics


class RepositoryJdbiStatisticsTest {
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
            repoStatistics.clear()
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

            var stats = repoStatistics.createStatistics(
                userId = user.id
            )

            println("Statistics criado com id = ${user.id}")

            val loaded = repoStatistics.findById(user.id)
            println("Statistics created: $loaded")


            assertNotNull(loaded)

            assertEquals(loaded!!.userId, user.id)
            assertEquals(loaded.gamesPlayed, 0)
            assertEquals(loaded.gamesWon, 0)

            val new = repoStatistics.incrementGamesPlayed(
                user.id
            )

            // Test = Save & Get Balance
            assertEquals(repoStatistics.findById(user.id)!!.gamesPlayed, 1)
        }
    }
}