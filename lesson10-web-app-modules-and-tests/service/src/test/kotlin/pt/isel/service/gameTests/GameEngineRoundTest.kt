package pt.isel.service.gameTests

import pt.isel.domain.Game.Face
import pt.isel.domain.Game.pokerDice.*
import pt.isel.domain.Game.Round.RoundState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GameEngineRoundTest {

    private val seq = generateSequence {
        listOf(Face.KING, Face.QUEEN, Face.JACK, Face.TEN, Face.NINE)
    }.flatten().iterator()
    private fun roll(): Face = seq.next()

    @Test
    fun full_round_and_advance() {
        var g = Game(id = 1, hostId = 10, ante = 5, maxPlayers = 3, matchId = 1)
        g = GameEngine.apply(g, Command.Join(10), ::roll)
        g = GameEngine.apply(g, Command.Join(11), ::roll)
        g = GameEngine.apply(g, Command.Start(10), ::roll)

        // P1
        g = GameEngine.apply(g, Command.Roll(10), ::roll)
        g = GameEngine.apply(g, Command.FinishTurn(10), ::roll)
        // P2
        g = GameEngine.apply(g, Command.Roll(11), ::roll)
        g = GameEngine.apply(g, Command.FinishTurn(11), ::roll)

        assertEquals(RoundState.SCORING, g.rounds.last().state)
        g = GameEngine.apply(g, Command.NextRound(10), ::roll)
        assertTrue(g.rounds[g.rounds.size - 2].state == RoundState.CLOSED)
    }

    @Test
    fun finishes_match_last_round() {
        var g = Game(id = 2, hostId = 1, ante = 2, maxPlayers = 2, totalRounds = 1, matchId = 2)
        g = GameEngine.apply(g, Command.Join(1), ::roll)
        g = GameEngine.apply(g, Command.Join(2), ::roll)
        g = GameEngine.apply(g, Command.Start(1), ::roll)
        g = GameEngine.apply(g, Command.Roll(1), ::roll)
        g = GameEngine.apply(g, Command.FinishTurn(1), ::roll)
        g = GameEngine.apply(g, Command.Roll(2), ::roll)
        g = GameEngine.apply(g, Command.FinishTurn(2), ::roll)
        g = GameEngine.apply(g, Command.NextRound(1), ::roll)
        assertEquals(GamePhase.FINISHED, g.phase)
        assertEquals(1, g.rounds.size)
    }
}
