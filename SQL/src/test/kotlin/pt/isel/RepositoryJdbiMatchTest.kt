package pt.isel

import org.jdbi.v3.core.Jdbi
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.postgresql.ds.PGSimpleDataSource
import pt.isel.domain.Game.Match.Match
import pt.isel.domain.Game.Match.MatchPlayer
import pt.isel.domain.Game.Match.MatchState
import pt.isel.domain.Game.Lobby.LobbyState
import pt.isel.domain.authentication.PasswordValidationInfo
import pt.isel.repo.jdbi.TransactionManagerJdbi
import pt.isel.repo.jdbi.configureWithAppRequirements
import java.time.Instant
import kotlin.test.*



class RepositoryJdbiMatchTest {
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
            // Limpar na ordem correta para não violar FKs
            repoMatch.clear()
            repoLobbies.clear()
            repoUsers.clear()
        }
        seedUsersLobbyAndMatch(trxManager)
    }

    @Test
    fun `createMatch e findById`() {
        trxManager.run {
            val host = repoUsers.createUser("Host", "host@isel.pt", PasswordValidationInfo("hash"))
            val player = repoUsers.createUser("Player", "player@isel.pt", PasswordValidationInfo("hash"))
            val lobby = repoLobbies.createLobby(
                lobbyHostId = host.id,
                name = "Lobby Test",
                description = "Desc Test",
                minPlayers = 2,
                maxPlayers = 4,
                rounds = 3,
                ante = 10
            )
            val players = listOf(
                MatchPlayer(userId = host.id, seatNo = 1, balanceAtStart = 1000, active = true),
                MatchPlayer(userId = player.id, seatNo = 2, balanceAtStart = 1000, active = true)
            )

            val match = repoMatch.createMatch(
                id = 1,
                lobbyId = lobby.id,
                players = players,
                totalRounds = 5,
                ante = 20
            )

            val found = repoMatch.findById(match.id)
            assertNotNull(found)
            assertEquals(1, found.id)
            assertEquals(lobby.id, found.lobbyId)
            assertEquals(5, found.totalRounds)
            assertEquals(20, found.ante)
            assertEquals(MatchState.RUNNING, found.state)
            assertEquals(2, found.players.size)
        }
    }

    @Test
    fun `updateState atualiza estado do match`() {
        trxManager.run {
            val host = repoUsers.createUser("Host", "host@isel.pt", PasswordValidationInfo("hash"))
            val lobby = repoLobbies.createLobby(host.id, "Lobby", "Desc", 2, 4, 3, 10)
            val players = listOf(
                MatchPlayer(userId = host.id, seatNo = 1, balanceAtStart = 1000, active = true)
            )

            val match = repoMatch.createMatch(1, lobby.id, players, 5, 20)

            val now = Instant.now()
            assertTrue(repoMatch.updateState(match.id, MatchState.FINISHED, now))

            val updated = repoMatch.findById(match.id)
            assertNotNull(updated)
            assertEquals(MatchState.FINISHED, updated.state)
            assertNotNull(updated.finishedAt)
        }
    }

    @Test
    fun `updateCurrentRound atualiza ronda atual`() {
        trxManager.run {
            val host = repoUsers.createUser("Host", "host@isel.pt", PasswordValidationInfo("hash"))
            val lobby = repoLobbies.createLobby(host.id, "Lobby", "Desc", 2, 4, 3, 10)
            val players = listOf(
                MatchPlayer(userId = host.id, seatNo = 1, balanceAtStart = 1000, active = true)
            )

            val match = repoMatch.createMatch(1, lobby.id, players, 5, 20)

            assertTrue(repoMatch.updateCurrentRound(match.id, 3))

            val updated = repoMatch.findById(match.id)
            assertNotNull(updated)
            assertEquals(3, updated.currentRoundNo)
        }
    }

    @Test
    fun `listPlayers retorna jogadores do match`() {
        trxManager.run {
            val host = repoUsers.createUser("Host", "host@isel.pt", PasswordValidationInfo("hash"))
            val player1 = repoUsers.createUser("Player1", "p1@isel.pt", PasswordValidationInfo("hash"))
            val player2 = repoUsers.createUser("Player2", "p2@isel.pt", PasswordValidationInfo("hash"))
            val lobby = repoLobbies.createLobby(host.id, "Lobby", "Desc", 2, 4, 3, 10)

            val players = listOf(
                MatchPlayer(userId = host.id, seatNo = 1, balanceAtStart = 1000, active = true),
                MatchPlayer(userId = player1.id, seatNo = 2, balanceAtStart = 1000, active = true),
                MatchPlayer(userId = player2.id, seatNo = 3, balanceAtStart = 1000, active = true)
            )

            val match = repoMatch.createMatch(1, lobby.id, players, 5, 20)

            val matchPlayers = repoMatch.listPlayers(match.id)
            assertEquals(3, matchPlayers.size)
            assertEquals(setOf(host.id, player1.id, player2.id), matchPlayers.map { it.userId }.toSet())
        }
    }

    @Test
    fun `addPlayer e removePlayer funcionam corretamente`() {
        trxManager.run {
            val host = repoUsers.createUser("Host", "host@isel.pt", PasswordValidationInfo("hash"))
            val player1 = repoUsers.createUser("Player1", "p1@isel.pt", PasswordValidationInfo("hash"))
            val player2 = repoUsers.createUser("Player2", "p2@isel.pt", PasswordValidationInfo("hash"))
            val lobby = repoLobbies.createLobby(host.id, "Lobby", "Desc", 2, 4, 3, 10)

            val initialPlayers = listOf(
                MatchPlayer(userId = host.id, seatNo = 1, balanceAtStart = 1000, active = true)
            )

            val match = repoMatch.createMatch(1, lobby.id, initialPlayers, 5, 20)

            // Adicionar jogador
            val newPlayer = MatchPlayer(userId = player1.id, seatNo = 2, balanceAtStart = 1000, active = true)
            assertTrue(repoMatch.addPlayer(match.id, newPlayer))

            var players = repoMatch.listPlayers(match.id)
            assertEquals(2, players.size)

            // Remover jogador
            assertTrue(repoMatch.removePlayer(match.id, player1.id))

            players = repoMatch.listPlayers(match.id)
            assertEquals(1, players.size)
            assertEquals(host.id, players[0].userId)
        }
    }

    @Test
    fun `save atualiza todos os campos do match`() {
        trxManager.run {
            val host = repoUsers.createUser("Host", "host@isel.pt", PasswordValidationInfo("hash"))
            val lobby = repoLobbies.createLobby(host.id, "Lobby", "Desc", 2, 4, 3, 10)
            val players = listOf(
                MatchPlayer(userId = host.id, seatNo = 1, balanceAtStart = 1000, active = true)
            )

            val match = repoMatch.createMatch(1, lobby.id, players, 5, 20)

            val now = Instant.now()
            val updated = Match(
                id = match.id,
                lobbyId = match.lobbyId,
                players = match.players,
                totalRounds = 10, // alterado
                ante = 50, // alterado
                state = MatchState.FINISHED, // alterado
                currentRoundNo = 3, // alterado
                startedAt = match.startedAt,
                finishedAt = now,
                rounds = emptyList()
            )

            repoMatch.save(updated)

            val found = repoMatch.findById(match.id)
            assertNotNull(found)
            assertEquals(10, found.totalRounds)
            assertEquals(50, found.ante)
            assertEquals(MatchState.FINISHED, found.state)
            assertEquals(3, found.currentRoundNo)
            assertNotNull(found.finishedAt)
        }
    }

    @Test
    fun `deleteById apaga match`() {
        trxManager.run {
            val host = repoUsers.createUser("Host", "host@isel.pt", PasswordValidationInfo("hash"))
            val lobby = repoLobbies.createLobby(host.id, "Lobby", "Desc", 2, 4, 3, 10)
            val players = listOf(
                MatchPlayer(userId = host.id, seatNo = 1, balanceAtStart = 1000, active = true)
            )

            val match = repoMatch.createMatch(1, lobby.id, players, 5, 20)

            assertTrue(repoMatch.deleteById(match.id))
            assertNull(repoMatch.findById(match.id))
        }
    }

    @Test
    fun `setPlayerActive altera estado de ativação do jogador`() {
        trxManager.run {
            val host = repoUsers.createUser("Host", "host@isel.pt", PasswordValidationInfo("hash"))
            val player = repoUsers.createUser("Player", "player@isel.pt", PasswordValidationInfo("hash"))
            val lobby = repoLobbies.createLobby(host.id, "Lobby", "Desc", 2, 4, 3, 10)

            val players = listOf(
                MatchPlayer(userId = host.id, seatNo = 1, balanceAtStart = 1000, active = true),
                MatchPlayer(userId = player.id, seatNo = 2, balanceAtStart = 1000, active = true)
            )

            val match = repoMatch.createMatch(1, lobby.id, players, 5, 20)

            assertEquals(1, repoMatch.setPlayerActive(match.id, player.id, false))

            val updatedPlayers = repoMatch.listPlayers(match.id)
            val playerState = updatedPlayers.find { it.userId == player.id }
            assertNotNull(playerState)
            assertFalse(playerState.active)
        }
    }

    @Test
    fun `clear apaga todos os Match`() {
        trxManager.run {
            val host = repoUsers.createUser("Host", "host@isel.pt", PasswordValidationInfo("hash"))
            val lobby = repoLobbies.createLobby(host.id, "Lobby", "Desc", 2, 4, 3, 10)

            val players = listOf(
                MatchPlayer(userId = host.id, seatNo = 1, balanceAtStart = 1000, active = true)
            )

            repoMatch.createMatch(1, lobby.id, players, 5, 20)
            repoMatch.createMatch(2, lobby.id, players, 6, 30)

            assertNotNull(repoMatch.findById(1))
            assertNotNull(repoMatch.findById(2))

            repoMatch.clear()

            assertNull(repoMatch.findById(1))
            assertNull(repoMatch.findById(2))
        }
    }

    @Test
    fun `exists verifica corretamente se o match existe`() {
        trxManager.run {
            val host = repoUsers.createUser("Host", "host@isel.pt", PasswordValidationInfo("hash"))
            val lobby = repoLobbies.createLobby(host.id, "Lobby", "Desc", 2, 4, 3, 10)
            val players = listOf(
                MatchPlayer(userId = host.id, seatNo = 1, balanceAtStart = 1000, active = true)
            )

            val match = repoMatch.createMatch(1, lobby.id, players, 5, 20)

            assertTrue(repoMatch.exists(match.id))
            assertFalse(repoMatch.exists(999))
        }
    }
}


fun seedUsersLobbyAndMatch(trxManager: TransactionManagerJdbi) {
    trxManager.run {
        // 1) Criar utilizadores
        val host = repoUsers.createUser(
            name = "Host",
            email = "host_${System.currentTimeMillis()}@isel.pt",
            passwordValidation = PasswordValidationInfo("hash")
        )
        val player1 = repoUsers.createUser(
            name = "Player1",
            email = "p1_${System.currentTimeMillis()}@isel.pt",
            passwordValidation = PasswordValidationInfo("hash")
        )
        val player2 = repoUsers.createUser(
            name = "Player2",
            email = "p2_${System.currentTimeMillis()}@isel.pt",
            passwordValidation = PasswordValidationInfo("hash")
        )

        // 2) Criar lobby com o id do host criado
        val lobbyId = repoLobbies.createLobby(
            lobbyHostId = host.id,
            name = "Lobby",
            description = "Desc",
            minPlayers = 2,
            maxPlayers = 4,
            rounds = 3,
            ante = 10
        ).id

        // 3) Criar match e respetivos players com os ids válidos
        val players = listOf(
            MatchPlayer(userId = host.id, seatNo = 1, balanceAtStart = 1000, active = true),
            MatchPlayer(userId = player1.id, seatNo = 2, balanceAtStart = 1000, active = true),
            MatchPlayer(userId = player2.id, seatNo = 3, balanceAtStart = 1000, active = true),
        )

        repoMatch.createMatch(
            id = 1,
            lobbyId = lobbyId,
            players = players,
            totalRounds = 5,
            ante = 20,
            state = MatchState.RUNNING,       // Adiciona estado
            currentRoundNo = 1,               // Adiciona número da rodada atual
            startedAt = Instant.now(),        // Adiciona timestamp de início
            finishedAt = null                 // Adiciona timestamp de finalização como null
        )

    }
}
