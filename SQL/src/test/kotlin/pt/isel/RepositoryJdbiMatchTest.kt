package pt.isel

import org.jdbi.v3.core.Jdbi
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.postgresql.ds.PGSimpleDataSource
import pt.isel.domain.Game.Match.Match
import pt.isel.domain.Game.Match.MatchPlayer
import pt.isel.domain.Game.Match.MatchState
import pt.isel.domain.authentication.PasswordValidationInfo
import pt.isel.repo.jdbi.TransactionManagerJdbi
import pt.isel.repo.jdbi.configureWithAppRequirements
import java.time.Instant
import kotlin.test.*


class RepositoryJdbiMatchTest {
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
            repoMatch.clear()
            repoLobbies.clear()
            repoUsers.clear()
        }
        seedUsersLobbyAndMatch(trxManager)
    }

    @Test
    fun `createMatch e findById`() {
        trxManager.run {
            val host = repoUsers.createUser("Host", "host@isel.pt", PasswordValidationInfo("hash"),
                invitationCode = "TODO()")
            val player = repoUsers.createUser("Player", "player@isel.pt", PasswordValidationInfo("hash"),
                invitationCode = "TODO()")
            val lobby = repoLobbies.createLobby(
                lobbyHostId = host.id,
                name = "Lobby Test",
                description = "Desc Test",
                minPlayers = 2,
                maxPlayers = 4,
                rounds = 3,
                ante = 10
            )
            val match = repoMatch.createMatch(
                lobbyId = lobby.id, totalRounds = 5, ante = 20, maxPlayers = 10
            )
            val players = listOf(
                MatchPlayer(
                    userId = host.id, seatNo = 1, balanceAtStart = 1000, active = true, matchId = match.id, turn = false
                ), MatchPlayer(
                    userId = player.id,
                    seatNo = 2,
                    balanceAtStart = 1000,
                    active = true,
                    matchId = match.id,
                    turn = false
                )
            )


            val found = repoMatch.findById(match.id)
            assertNotNull(found)
            assertEquals(2, found.id)
            assertEquals(lobby.id, found.lobbyId)
            assertEquals(5, found.totalRounds)
            assertEquals(20, found.ante)
            assertEquals(MatchState.RUNNING, found.state)
        }
    }

    @Test
    fun `updateState atualiza estado do match`() {
        trxManager.run {
            val host = repoUsers.createUser("Host", "host@isel.pt", PasswordValidationInfo("hash"),
                invitationCode = "TODO()")
            val lobby = repoLobbies.createLobby(host.id, "Lobby", "Desc", 2, 4, 3, 10)
            val match = repoMatch.createMatch(lobby.id, 5, 20, maxPlayers = 10)

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
            val host = repoUsers.createUser("Host", "host@isel.pt", PasswordValidationInfo("hash"),
                invitationCode = "TODO()")
            val lobby = repoLobbies.createLobby(host.id, "Lobby", "Desc", 2, 4, 3, 10)

            val match = repoMatch.createMatch(lobby.id, 5, 20, maxPlayers = 10)

            assertTrue(repoMatch.updateCurrentRound(match.id, 3))

            val updated = repoMatch.findById(match.id)
            assertNotNull(updated)
            assertEquals(3, updated.currentRoundNo)
        }
    }

    @Test
    fun `listPlayers retorna jogadores do match`() {
        trxManager.run {
            val host = repoUsers.createUser("Host", "host@isel.pt", PasswordValidationInfo("hash"),
                invitationCode = "TODO()")
            val player1 = repoUsers.createUser("Player1", "p1@isel.pt", PasswordValidationInfo("hash"),
                invitationCode = "TODO()")
            val player2 = repoUsers.createUser("Player2", "p2@isel.pt", PasswordValidationInfo("hash"),
                invitationCode = "TODO()")
            val lobby = repoLobbies.createLobby(host.id, "Lobby", "Desc", 2, 4, 3, 10)

            val match = repoMatch.createMatch(lobby.id, 5, 20, maxPlayers = 10)

            repoMatch.addPlayer(
                matchId = match.id,
                userId = host.id, balanceAtStart = 1000,
                seatNo = repoMatch.getMaxSeatNo(match.id)+1
            )
            repoMatch.addPlayer(
                matchId = match.id,
                userId = player1.id, balanceAtStart = 1000,
                seatNo = repoMatch.getMaxSeatNo(match.id)+1
            )
            repoMatch.addPlayer(
                matchId = match.id,
                userId = player2.id, balanceAtStart = 1000,
                seatNo = repoMatch.getMaxSeatNo(match.id)+1
            )


            val matchPlayers = repoMatch.listPlayers(match.id)
            assertEquals(3, matchPlayers.size)
            assertEquals(setOf(host.id, player1.id, player2.id), matchPlayers.map { it.userId }.toSet())
        }
    }

    @Test
    fun `addPlayer e removePlayer funcionam corretamente`() {
        trxManager.run {
            val host = repoUsers.createUser("Host", "host@isel.pt", PasswordValidationInfo("hash"),
                invitationCode = "TODO()")
            val player1 = repoUsers.createUser("Player1", "p1@isel.pt", PasswordValidationInfo("hash"),
                invitationCode = "TODO()")
            val lobby = repoLobbies.createLobby(host.id, "Lobby", "Desc", 2, 4, 3, 10)

            val match = repoMatch.createMatch(lobby.id, 5, 20, maxPlayers = 10)


            assertTrue(
                repoMatch.addPlayer(
                    match.id,
                    userId = player1.id, balanceAtStart = 1000,
                    seatNo = repoMatch.getMaxSeatNo(match.id)+1
                )
            )

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
            val host = repoUsers.createUser("Host", "host@isel.pt", PasswordValidationInfo("hash"),
                invitationCode = "TODO()")
            val lobby = repoLobbies.createLobby(host.id, "Lobby", "Desc", 2, 4, 3, 10)
            val match = repoMatch.createMatch(lobby.id, 5, 20, maxPlayers = 10)

            MatchPlayer(
                userId = host.id, seatNo = 1, balanceAtStart = 1000, active = true, matchId = match.id, turn = false
            )


            val now = Instant.now()
            val updated = Match(
                id = match.id, lobbyId = match.lobbyId, totalRounds = 10, // alterado
                ante = 50, // alterado
                state = MatchState.FINISHED, // alterado
                currentRoundNo = 3, // alterado
                startedAt = match.startedAt, finishedAt = now, rounds = emptyList(), maxPlayers = 10
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
            val host = repoUsers.createUser("Host", "host@isel.pt", PasswordValidationInfo("hash"),
                invitationCode = "TODO()")
            val lobby = repoLobbies.createLobby(host.id, "Lobby", "Desc", 2, 4, 3, 10)
            val match = repoMatch.createMatch(lobby.id, 5, 20, maxPlayers = 10)

            assertTrue(repoMatch.deleteById(match.id))
            assertNull(repoMatch.findById(match.id))
        }
    }

    @Test
    fun `setPlayerActive altera estado de ativação do jogador`() {
        trxManager.run {
            val host = repoUsers.createUser("Host", "host@isel.pt", PasswordValidationInfo("hash"),
                invitationCode = "TODO()")
            val player = repoUsers.createUser("Player", "player@isel.pt", PasswordValidationInfo("hash"),
                invitationCode = "TODO()")
            val lobby = repoLobbies.createLobby(host.id, "Lobby", "Desc", 2, 4, 3, 10)

            val match = repoMatch.createMatch(lobby.id, 5, 20, maxPlayers = 10)

            repoMatch.addPlayer(
                matchId = match.id,
                userId = host.id, balanceAtStart = 1000,
                seatNo = repoMatch.getMaxSeatNo(match.id)+1
            )
            repoMatch.addPlayer(
                matchId = match.id,
                userId = player.id,
                balanceAtStart = 1000,
                seatNo = repoMatch.getMaxSeatNo(match.id)+1
                )




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
            val host = repoUsers.createUser("Host", "host@isel.pt", PasswordValidationInfo("hash"),
                invitationCode = "TODO()")
            val lobby = repoLobbies.createLobby(host.id, "Lobby", "Desc", 2, 4, 3, 10)

            var match1 = repoMatch.createMatch(lobby.id, 5, 20, maxPlayers = 10)

            MatchPlayer(
                userId = host.id, seatNo = 1, balanceAtStart = 1000, active = true, matchId = match1.id, turn = false
            )


            var match2 = repoMatch.createMatch(lobby.id, 6, 30, maxPlayers = 10)

            assertNotNull(repoMatch.findById(match1.id))
            assertNotNull(repoMatch.findById(match2.id))

            repoMatch.clear()

            assertNull(repoMatch.findById(match1.id))
            assertNull(repoMatch.findById(match2.id))
        }
    }

    @Test
    fun `exists verifica corretamente se o match existe`() {
        trxManager.run {
            val host = repoUsers.createUser("Host", "host@isel.pt", PasswordValidationInfo("hash"),
                invitationCode = "TODO()")
            val lobby = repoLobbies.createLobby(host.id, "Lobby", "Desc", 2, 4, 3, 10)
            val match = repoMatch.createMatch(lobby.id, 5, 20, maxPlayers = 10)

            MatchPlayer(
                userId = host.id, seatNo = 1, balanceAtStart = 1000, active = true, matchId = match.id, turn = false
            )



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
            passwordValidation = PasswordValidationInfo("hash"),
            invitationCode = "TODO()"
        )
        val player1 = repoUsers.createUser(
            name = "Player1",
            email = "p1_${System.currentTimeMillis()}@isel.pt",
            passwordValidation = PasswordValidationInfo("hash"),
            invitationCode = "TODO()"
        )
        val player2 = repoUsers.createUser(
            name = "Player2",
            email = "p2_${System.currentTimeMillis()}@isel.pt",
            passwordValidation = PasswordValidationInfo("hash"),
            invitationCode = "TODO()"
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

        repoMatch.createMatch(
            lobbyId = lobbyId, totalRounds = 5, ante = 20, state = MatchState.RUNNING,       // Adiciona estado
            currentRoundNo = 1,               // Adiciona número da rodada atual
            startedAt = Instant.now(),        // Adiciona timestamp de início
            finishedAt = null                 // Adiciona timestamp de finalização como null
            , maxPlayers = 10
        )

    }
}
