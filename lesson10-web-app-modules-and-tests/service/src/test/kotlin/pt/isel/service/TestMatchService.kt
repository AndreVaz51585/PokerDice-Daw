/*
package pt.isel.service

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import pt.isel.domain.Game.Match.Match
import pt.isel.domain.Game.Match.MatchPlayer
import pt.isel.domain.Game.Match.MatchState
import pt.isel.repo.RepositoryMatch
import pt.isel.repo.TransactionManager
import pt.isel.service.Auxiliary.success
import pt.isel.service.Auxiliary.failure
import pt.isel.service.matchService.MatchServiceError
import pt.isel.service.matchService.MatchServiceImpl

class TestMatchService {

    private lateinit var matchService: MatchServiceImpl
    private lateinit var repoMatch: RepositoryMatch
    private lateinit var transactionManager: TransactionManager

    @BeforeEach
    fun setup() {
        repoMatch = RepositoryMatch()
        transactionManager = TransactionManager()
        matchService = MatchServiceImpl(repoMatch, transactionManager)
    }

    @Test
    fun `createMatch with valid data returns success`() {
        // Arrange
        val players = listOf(
            MatchPlayer(userId = 1, balance = 100),
            MatchPlayer(userId = 2, balance = 100)
        )

        // Act
        val result = matchService.createMatch(1, 10, players, 5, 10)

        // Assert
        assertTrue(result.isSuccess())
        val match = result.value
        assertEquals(1, match.id)
        assertEquals(10, match.lobbyId)
        assertEquals(5, match.totalRounds)
        assertEquals(10, match.ante)
        assertEquals(players, match.players)
    }

    @Test
    fun `createMatch with empty players returns failure`() {
        // Act
        val result = matchService.createMatch(1, 10, emptyList(), 5, 10)

        // Assert
        assertTrue(result.isFailure())
        assertEquals(MatchServiceError.InvalidState, result.error)
    }

    @Test
    fun `createMatch with duplicate players returns failure`() {
        // Arrange
        val players = listOf(
            MatchPlayer(userId = 1, balance = 100),
            MatchPlayer(userId = 1, balance = 200) // Duplicado
        )

        // Act
        val result = matchService.createMatch(1, 10, players, 5, 10)

        // Assert
        assertTrue(result.isFailure())
        assertEquals(MatchServiceError.PlayerAlreadyInMatch, result.error)
    }

    @Test
    fun `getMatch with existing id returns success`() {
        // Arrange
        val match = createSampleMatch()
        repoMatch.createMatch(match.lobbyId, match.players, match.totalRounds, match.ante, match.id)

        // Act
        val result = matchService.getMatch(match.id)

        // Assert
        assertTrue(result.isSuccess())
        assertEquals(match.id, result.value.id)
    }

    @Test
    fun `getMatch with non-existing id returns failure`() {
        // Act
        val result = matchService.getMatch(999)

        // Assert
        assertTrue(result.isFailure())
        assertEquals(MatchServiceError.MatchNotFound, result.error)
    }

    @Test
    fun `addPlayer to existing match returns success`() {
        // Arrange
        val match = createSampleMatch()
        repoMatch.createMatch(match.lobbyId, match.players, match.totalRounds, match.ante, match.id)
        val newPlayer = MatchPlayer(userId = 99, balance = 100)

        // Act
        val result = matchService.addPlayer(match.id, newPlayer)

        // Assert
        assertTrue(result.isSuccess())
        assertTrue(result.value)
        val updatedPlayers = repoMatch.listPlayers(match.id)
        assertTrue(updatedPlayers.any { it.userId == newPlayer.userId })
    }

    @Test
    fun `addPlayer to full match returns failure`() {
        // Arrange
        val players = listOf(
            MatchPlayer(userId = 1, balance = 100),
            MatchPlayer(userId = 2, balance = 100)
        )
        val match = Match(
            id = 1,
            lobbyId = 10,
            players = players,
            totalRounds = 5,
            ante = 10,
            state = MatchState.WAITING,
            maxPlayers = 2
        )
        repoMatch.matches[match.id] = match

        // Act
        val result = matchService.addPlayer(match.id, MatchPlayer(userId = 3, balance = 100))

        // Assert
        assertTrue(result.isFailure())
        assertEquals(MatchServiceError.MatchFull, result.error)
    }

    @Test
    fun `removePlayer from match returns success`() {
        // Arrange
        val playerToRemove = MatchPlayer(userId = 2, balance = 100)
        val match = createSampleMatchWithPlayers(
            listOf(
                MatchPlayer(userId = 1, balance = 100),
                playerToRemove
            )
        )

        // Act
        val result = matchService.removePlayer(match.id, playerToRemove.userId)

        // Assert
        assertTrue(result.isSuccess())
        assertTrue(result.value)
        val updatedPlayers = repoMatch.listPlayers(match.id)
        assertFalse(updatedPlayers.any { it.userId == playerToRemove.userId })
    }

    @Test
    fun `updateState with valid transition returns success`() {
        // Arrange
        val match = createSampleMatch()
        repoMatch.createMatch(match.lobbyId, match.players, match.totalRounds, match.ante, match.id)

        // Act
        val result = matchService.updateState(match.id, MatchState.IN_PROGRESS)

        // Assert
        assertTrue(result.isSuccess())
        assertTrue(result.value)
        val updatedMatch = repoMatch.findById(match.id)
        assertEquals(MatchState.IN_PROGRESS, updatedMatch?.state)
    }

    // Métodos auxiliares
    private fun createSampleMatch() = Match(
        id = 1,
        lobbyId = 10,
        players = listOf(MatchPlayer(userId = 1, balance = 100)),
        totalRounds = 5,
        ante = 10,
        state = MatchState.WAITING
    )

    private fun createSampleMatchWithPlayers(players: List<MatchPlayer>): Match {
        val match = Match(
            id = 1,
            lobbyId = 10,
            players = players,
            totalRounds = 5,
            ante = 10,
            state = MatchState.WAITING
        )
        repoMatch.matches[match.id] = match
        return match
    }
}
*/
