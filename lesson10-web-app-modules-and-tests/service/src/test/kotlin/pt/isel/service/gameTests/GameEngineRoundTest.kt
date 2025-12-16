package pt.isel.service.gameTests

import pt.isel.domain.Game.Face
import pt.isel.domain.Game.Round.RoundState
import pt.isel.domain.Game.pokerDice.Command
import pt.isel.domain.Game.pokerDice.Game
import pt.isel.domain.Game.pokerDice.GameEngine
import pt.isel.domain.Game.pokerDice.GamePhase
import kotlin.test.Test
import kotlin.test.assertEquals

class GameEngineRoundTest {
    private val seq =
        generateSequence {
            listOf(Face.KING, Face.QUEEN, Face.JACK, Face.TEN, Face.NINE)
        }.flatten().iterator()

    private fun roll(): Face = seq.next()

    @Test
    fun full_round_and_advance() {
        var g = Game(matchId = 1, hostId = 10, ante = 5, maxPlayers = 3)
        g = GameEngine.apply(g, Command.Join(10))
        g = GameEngine.apply(g, Command.Join(11))
        g = GameEngine.apply(g, Command.Start(10))

        // P1
        g = GameEngine.apply(g, Command.Roll(10))
        g = GameEngine.apply(g, Command.FinishTurn(10))
        // P2
        g = GameEngine.apply(g, Command.Roll(11))
        g = GameEngine.apply(g, Command.FinishTurn(11))

        assertEquals(RoundState.SCORING, g.rounds.last().state)
        g = GameEngine.apply(g, Command.NextRound(10))
        assertEquals(g.rounds[g.rounds.size - 2].state, RoundState.CLOSED)
    }

    @Test
    fun finishes_match_last_round() {
        var g = Game(matchId = 2, hostId = 1, ante = 2, maxPlayers = 2, totalRounds = 1)
        g = GameEngine.apply(g, Command.Join(1))
        g = GameEngine.apply(g, Command.Join(2))
        g = GameEngine.apply(g, Command.Start(1))
        g = GameEngine.apply(g, Command.Roll(1))
        g = GameEngine.apply(g, Command.FinishTurn(1))
        g = GameEngine.apply(g, Command.Roll(2))
        g = GameEngine.apply(g, Command.FinishTurn(2))
        g = GameEngine.apply(g, Command.NextRound(1))
        assertEquals(GamePhase.FINISHED, g.phase)
        assertEquals(1, g.rounds.size)
    }
}
