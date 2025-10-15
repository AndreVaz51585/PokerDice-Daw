package pt.isel

import org.jdbi.v3.core.Jdbi
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.postgresql.ds.PGSimpleDataSource
import pt.isel.domain.Game.Lobby.Lobby
import pt.isel.domain.Game.Lobby.LobbyState
import pt.isel.domain.authentication.PasswordValidationInfo
import pt.isel.repo.jdbi.TransactionManagerJdbi
import pt.isel.repo.jdbi.configureWithAppRequirements
import kotlin.test.*

class RepositoryJdbiLobbyTest {
    companion object {
        private val jdbi =
            Jdbi
                .create(
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
            // limpar primeiro lobbies (para não violar FKs), depois users
            repoLobbies.clear()
            repoUsers.clear()
        }
    }

    @Test
    fun `createLobby e findById`() {
        trxManager.run {
            val host = repoUsers.createUser("Host", "host@isel.pt", PasswordValidationInfo("hash"))
            val lobby = repoLobbies.createLobby(
                lobbyHostId = host.id,
                name = "Lobby A",
                description = "Desc A",
                minPlayers = 2,
                maxPlayers = 4,
                rounds = 3,
                ante = 1
            )
            val found = repoLobbies.findById(lobby.id)
            assertNotNull(found)
            assertEquals(lobby.id, found.id)
            assertEquals("Lobby A", found.name)
            assertEquals(LobbyState.OPEN, found.state)
        }
    }

    @Test
    fun `save atualiza campos do lobby`() {
        trxManager.run {
            val host = repoUsers.createUser("Host", "host@isel.pt", PasswordValidationInfo("hash"))
            val lobby = repoLobbies.createLobby(host.id, "L0", "D0", 2, 4, 3, 1)
            val updated = lobby.copy(
                name = "L1",
                description = "D1",
                minPlayers = 3,
                maxPlayers = 5,
                rounds = 4,
                ante = 2,
                state = LobbyState.CLOSED
            )
            repoLobbies.save(updated)
            val found = repoLobbies.findById(lobby.id)
            assertNotNull(found)
            assertEquals("L1", found.name)
            assertEquals("D1", found.description)
            assertEquals(3, found.minPlayers)
            assertEquals(5, found.maxPlayers)
            assertEquals(4, found.rounds)
            assertEquals(2, found.ante)
            assertEquals(LobbyState.CLOSED, found.state)
        }
    }

    @Test
    fun `listAllOpenLobbies com paginacao`() {
        trxManager.run {
            val h1 = repoUsers.createUser("H1", "h1@isel.pt", PasswordValidationInfo("x"))
            val h2 = repoUsers.createUser("H2", "h2@isel.pt", PasswordValidationInfo("x"))
            val h3 = repoUsers.createUser("H3", "h3@isel.pt", PasswordValidationInfo("x"))
            val h4 = repoUsers.createUser("H4", "h4@isel.pt", PasswordValidationInfo("x"))

            repoLobbies.createLobby(h1.id, "O1", "d", 2, 4, 3, 1)
            repoLobbies.createLobby(h2.id, "C1", "d", 2, 4, 3, 1) // quero closed
            repoLobbies.createLobby(h3.id, "O2", "d", 2, 4, 3, 1)
            repoLobbies.createLobby(h4.id, "O3", "d", 2, 4, 3, 1)

            repoLobbies.save(Lobby(2, "C1", "d", h2.id, 2, 4, 3, 1, LobbyState.CLOSED))
            repoLobbies.save(Lobby(4, "O3", "d", h4.id, 2, 4, 3, ante = 1, LobbyState.CLOSED))

            val p1 = repoLobbies.listAllOpenLobbies(limit = 2, offset = 0)
            val p2 = repoLobbies.listAllOpenLobbies(limit = 2, offset = 2)

            assertEquals(2, p1.size)
            assertTrue(p1.all { it.state == LobbyState.OPEN })
            assertTrue(p2.all { it.state == LobbyState.OPEN })
            assertTrue((p1 + p2).none { it.name == "C1" })
        }
    }

    @Test
    fun `gestao de players add list count remove`() {
        trxManager.run {
            val host = repoUsers.createUser("Host", "host@isel.pt", PasswordValidationInfo("h"))
            val u1 = repoUsers.createUser("U1", "u1@isel.pt", PasswordValidationInfo("h"))
            val u2 = repoUsers.createUser("U2", "u2@isel.pt", PasswordValidationInfo("h"))
            val lobby = repoLobbies.createLobby(host.id, "Lobby P", "D", 2, 4, 3, 1)

            // o trigger pode já ter inserido o host; ignora o resultado
            repoLobbies.addPlayerToLobby(lobby.id, host.id)

            assertTrue(repoLobbies.addPlayerToLobby(lobby.id, u1.id))
            assertTrue(repoLobbies.addPlayerToLobby(lobby.id, u2.id))
            // idempotente
            assertFalse(repoLobbies.addPlayerToLobby(lobby.id, u2.id))

            val count = repoLobbies.countPlayers(lobby.id)
            assertEquals(3, count)

            val players = repoLobbies.listPlayers(lobby.id)
            assertEquals(listOf(host.id, u1.id, u2.id), players.map { it.id })

            val removed = repoLobbies.remove(lobby.id, u1.id)
            assertEquals(1, removed)
            assertEquals(2, repoLobbies.countPlayers(lobby.id))
        }
    }


    @Test
    fun `getLobbyHost devolve o user do host`() {
        trxManager.run {
            val host = repoUsers.createUser("Host", "host@isel.pt", PasswordValidationInfo("h"))
            val lobby = repoLobbies.createLobby(host.id, "Lobby H", "D", 2, 4, 3, 1)
            val foundHost = repoLobbies.getLobbyHost(lobby)
            assertNotNull(foundHost)
            assertEquals(host.id, foundHost.id)
        }
    }

    @Test
    fun `deleteById apaga lobby`() {
        trxManager.run {
            val host = repoUsers.createUser("Host", "host@isel.pt", PasswordValidationInfo("h"))
            val lobby = repoLobbies.createLobby(host.id, "Lobby D", "D", 2, 4, 3, 1)
            assertNotNull(repoLobbies.findById(lobby.id))
            repoLobbies.deleteById(lobby.id)
            assertNull(repoLobbies.findById(lobby.id))
        }
    }

    @Test
    fun `clear apaga todos os lobbies`() {
        trxManager.run {
            val h1 = repoUsers.createUser("H1", "h1@isel.pt", PasswordValidationInfo("x"))
            val h2 = repoUsers.createUser("H2", "h2@isel.pt", PasswordValidationInfo("x"))
            repoLobbies.createLobby(h1.id, "L1", "d", 2, 4, 3, 1)
            repoLobbies.createLobby(h2.id, "L2", "d", 2, 4, 3, 1)
            assertTrue(repoLobbies.findAll().isNotEmpty())
            repoLobbies.clear()
            assertTrue(repoLobbies.findAll().isEmpty())
        }
    }
}
