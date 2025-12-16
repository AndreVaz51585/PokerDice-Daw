package pt.isel

import org.jdbi.v3.core.Jdbi
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.postgresql.ds.PGSimpleDataSource
import pt.isel.domain.Game.Face
import pt.isel.domain.Game.Hand
import pt.isel.domain.Game.Round.Round
import pt.isel.domain.Game.Round.RoundState
import pt.isel.domain.authentication.PasswordValidationInfo
import pt.isel.repo.jdbi.TransactionManagerJdbi
import pt.isel.repo.jdbi.configureWithAppRequirements
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RepositoryJdbiRoundTest {
    companion object {
        private val jdbi =
            Jdbi.create(
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
            repoUsers.createUser(
                name = "João",
                email = "joao@gmail.com",
                passwordValidation = PasswordValidationInfo(validationInfo = "hashedPassword"),
                invitationCode = "TODO()",
            )

            repoLobbies.createLobby(
                name = "Test Lobby",
                lobbyHostId = 1,
                description = "Test Description",
                minPlayers = 2,
                maxPlayers = 5,
                rounds = 5,
                ante = 10,
            )

            // Arrange: criar uma match válida
            val match =
                repoMatch.createMatch(
                    lobbyId = 1,
                    totalRounds = 3,
                    ante = 5,
                    maxPlayers = 10,
                )

            // Act: criar o round e obter por id
            val created =
                repoRound.createRound(
                    matchId = match.id,
                    number = 1,
                    anteCoins = match.ante,
                    startedAt = Instant.now(),
                )
            println("Round criado com id = ${created.id}")

            val loaded = repoRound.findById(created.id.toInt())
            println("Loaded round: $loaded")
            println("Created round: $created")

            // Assert: validar campos essenciais
            assertNotNull(loaded)

            assertEquals(created.id, loaded.id)
            assertEquals(1, loaded.number)
            // assertEquals(match.id, loaded.matchId)
            assertEquals(match.ante, loaded.anteCoins)
        }
    }

    @Test
    fun `updateState atualiza estado do round`() {
        trxManager.run {
            // Preparação: criar usuário, lobby e match
            val user =
                repoUsers.createUser(
                    name = "João",
                    email = "joao@gmail.com",
                    passwordValidation = PasswordValidationInfo(validationInfo = "hashedPassword"),
                    invitationCode = "TODO()",
                )

            val lobby =
                repoLobbies.createLobby(
                    name = "Test Lobby",
                    lobbyHostId = user.id,
                    description = "Test Description",
                    minPlayers = 2,
                    maxPlayers = 5,
                    rounds = 5,
                    ante = 10,
                )

            val match =
                repoMatch.createMatch(
                    lobbyId = lobby.id,
                    totalRounds = 3,
                    ante = 5,
                    maxPlayers = 10,
                )

            // Criar um round inicial
            val round =
                repoRound.createRound(
                    matchId = match.id,
                    number = 1,
                    anteCoins = match.ante,
                    startedAt = Instant.now(),
                )

            // Verificar estado inicial
            val originalRound = repoRound.findById(round.id.toInt())
            assertNotNull(originalRound)

            // Act: atualizar o estado do round para FINISHED
            val success = repoRound.updateState(round.id, RoundState.CLOSED)
            assertTrue(success)

            // Assert: verificar que o estado foi atualizado
            val updatedRound = repoRound.findById(round.id.toInt())
            assertNotNull(updatedRound)
            assertEquals(RoundState.CLOSED, updatedRound.state)
        }
    }

    @Test
    fun `save atualiza todos os campos do round`() {
        trxManager.run {
            // Preparação: criar usuário, lobby, match e round
            val user =
                repoUsers.createUser(
                    name = "João",
                    email = "joao@gmail.com",
                    passwordValidation = PasswordValidationInfo(validationInfo = "hashedPassword"),
                    invitationCode = "TODO()",
                )
            val user2 =
                repoUsers.createUser(
                    name = "Joãoo",
                    email = "joaoo@gmail.com",
                    passwordValidation = PasswordValidationInfo(validationInfo = "hashedPassword"),
                    invitationCode = "TODO()",
                )

            val lobby =
                repoLobbies.createLobby(
                    name = "Test Lobby",
                    lobbyHostId = user.id,
                    description = "Test Description",
                    minPlayers = 2,
                    maxPlayers = 5,
                    rounds = 5,
                    ante = 10,
                )

            val match =
                repoMatch.createMatch(
                    lobbyId = lobby.id,
                    totalRounds = 3,
                    ante = 5,
                    maxPlayers = 10,
                )

            val round =
                repoRound.createRound(
                    matchId = match.id,
                    number = 1,
                    anteCoins = match.ante,
                    startedAt = Instant.now(),
                )

            // Atualizar os campos do round
            val updatedRound =
                round.copy(
                    state = RoundState.CLOSED,
                    pot = 50,
                    winners = listOf(1, 2),
                    hands =
                        mapOf(
                            1 to Hand(faces = listOf(Face.ACE, Face.KING, Face.QUEEN, Face.JACK, Face.TEN)),
                            2 to Hand(faces = listOf(Face.NINE, Face.TEN, Face.JACK, Face.QUEEN, Face.KING)),
                        ),
                )
            repoRound.save(updatedRound)

            // Verificar se os campos foram atualizados
            val savedRound = repoRound.findById(round.id.toInt())
            val roundFinal =
                Round(
                    id = savedRound!!.id,
                    matchId = savedRound.matchId,
                    number = savedRound.number,
                    state = savedRound.state,
                    anteCoins = savedRound.anteCoins,
                    pot = savedRound.pot,
                    winners = repoRound.findWinnersByRoundId(round.id),
                )

            assertNotNull(savedRound)
            assertEquals(RoundState.CLOSED, roundFinal.state)
            assertEquals(50, roundFinal.pot)
            assertEquals(listOf(1, 2), roundFinal.winners)
        }
    }

    @Test
    fun `deleteById apaga round`() {
        trxManager.run {
            // Criar usuário para associar ao lobby
            val user =
                repoUsers.createUser(
                    name = "Usuário Teste",
                    email = "usuario@teste.com",
                    passwordValidation = PasswordValidationInfo("senha"),
                    invitationCode = "TODO()",
                )

            // Criar lobby para associar à match
            val lobby =
                repoLobbies.createLobby(
                    name = "Lobby Teste",
                    lobbyHostId = user.id,
                    description = "Descrição teste",
                    minPlayers = 2,
                    maxPlayers = 4,
                    rounds = 3,
                    ante = 5,
                )

            // Criar match para associar ao round
            val match =
                repoMatch.createMatch(
                    lobbyId = lobby.id,
                    totalRounds = 3,
                    ante = 5,
                    maxPlayers = 10,
                )

            // Criar um round
            val round =
                repoRound.createRound(
                    matchId = match.id,
                    number = 1,
                    anteCoins = match.ante,
                    startedAt = Instant.now(),
                )

            // Verificar que o round existe
            assertNotNull(repoRound.findById(round.id.toInt()))

            // Deletar o round
            val deleted = repoRound.deleteById(round.id.toInt())

            // Verificar que a operação teve sucesso
            assertTrue(deleted)

            // Verificar que o round não existe mais
            assertNull(repoRound.findById(round.id.toInt()))
        }
    }

    @Test
    fun `clear apaga todos os Round`() {
        trxManager.run {
            // Criar usuário para associar ao lobby
            val user =
                repoUsers.createUser(
                    name = "Usuário Teste",
                    email = "usuario@teste.com",
                    passwordValidation = PasswordValidationInfo("senha"),
                    invitationCode = "TODO()",
                )

            // Criar lobby para associar à match
            val lobby =
                repoLobbies.createLobby(
                    name = "Lobby Teste",
                    lobbyHostId = user.id,
                    description = "Descrição teste",
                    minPlayers = 2,
                    maxPlayers = 4,
                    rounds = 3,
                    ante = 5,
                )

            // Criar match para associar aos rounds
            val match =
                repoMatch.createMatch(
                    lobbyId = lobby.id,
                    totalRounds = 3,
                    ante = 5,
                    maxPlayers = 10,
                )

            // Criar dois rounds diferentes
            repoRound.createRound(
                matchId = match.id,
                number = 1,
                anteCoins = match.ante,
                startedAt = Instant.now(),
            )

            repoRound.createRound(
                matchId = match.id,
                number = 2,
                anteCoins = match.ante,
                startedAt = Instant.now(),
            )

            // Verificar que existem rounds
            assertTrue(repoRound.findAll().isNotEmpty())

            // Limpar todos os rounds
            repoRound.clear()

            // Verificar que não existem mais rounds
            assertTrue(repoRound.findAll().isEmpty())
        }
    }

    @Test
    fun `exists verifica corretamente se o round existe`() {
        trxManager.run {
            // Criar usuário para associar ao lobby
            val user =
                repoUsers.createUser(
                    name = "Usuário Teste",
                    email = "usuario@teste.com",
                    passwordValidation = PasswordValidationInfo("senha"),
                    invitationCode = "TODO()",
                )

            // Criar lobby para associar à match
            val lobby =
                repoLobbies.createLobby(
                    name = "Lobby Teste",
                    lobbyHostId = user.id,
                    description = "Descrição teste",
                    minPlayers = 2,
                    maxPlayers = 4,
                    rounds = 3,
                    ante = 5,
                )

            // Criar match para associar ao round
            val match =
                repoMatch.createMatch(
                    lobbyId = lobby.id,
                    totalRounds = 3,
                    ante = 5,
                    maxPlayers = 10,
                )

            // Criar um round
            val round =
                repoRound.createRound(
                    matchId = match.id,
                    number = 1,
                    anteCoins = match.ante,
                    startedAt = Instant.now(),
                )

            // Verificar que o round existe usando findById (não nulo indica existência)
            assertNotNull(repoRound.findById(round.id.toInt()))

            // Verificar que um ID inexistente retorna nulo
            assertNull(repoRound.findById(-1))

            // Deletar o round
            val deleted = repoRound.deleteById(round.id.toInt())
            assertTrue(deleted)

            // Verificar que o round não existe mais
            assertNull(repoRound.findById(round.id.toInt()))
        }
    }
}
