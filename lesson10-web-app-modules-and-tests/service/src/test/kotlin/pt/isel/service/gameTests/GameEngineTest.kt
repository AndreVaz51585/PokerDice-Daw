package pt.isel.service.gameTests

import pt.isel.domain.Game.Face
import pt.isel.domain.Game.Round.RoundState
import pt.isel.domain.Game.pokerDice.*
import kotlin.test.Test
import kotlin.test.assertEquals

class GameEngineTest {

    private fun fixedFaces(vararg f: Face) = object {
        val seq = f.toList()
        var i = 0
        fun roll(): Face = seq[i++ % seq.size]
    }

    @Test
    fun join_and_start() {
        var g = Game(id = 1, hostId = 10, ante = 5, maxPlayers = 4)
        g = GameEngine.apply(g, Command.Join(10)) { Face.ACE }
        g = GameEngine.apply(g, Command.Join(11)) { Face.ACE }
        g = GameEngine.apply(g, Command.Start(10)) { Face.ACE }
        assertEquals(GamePhase.ROLLING, g.phase)
        assertEquals(1, g.rounds.last().number)
        assertEquals(10, g.rounds.last().pot)
    }

    @Test
    fun full_round_to_finish() {
        val roller = fixedFaces(Face.ACE, Face.KING, Face.QUEEN, Face.JACK, Face.TEN)
        var g = Game(id = 1, hostId = 1, ante = 2, maxPlayers = 3)
        g = GameEngine.apply(g, Command.Join(1)) { roller.roll() }
        g = GameEngine.apply(g, Command.Join(2)) { roller.roll() }
        g = GameEngine.apply(g, Command.Start(1)) { roller.roll() }

        g = GameEngine.apply(g, Command.Roll(1)) { roller.roll() }
        g = GameEngine.apply(g, Command.FinishTurn(1)) { roller.roll() }
        g = GameEngine.apply(g, Command.Roll(2)) { roller.roll() }
        g = GameEngine.apply(g, Command.FinishTurn(2)) { roller.roll() }

        assertEquals(RoundState.SCORING, g.rounds.last().state)
        g = GameEngine.apply(g, Command.NextRound(1)) { roller.roll() }
        assertEquals(RoundState.CLOSED, g.rounds[g.rounds.size - 2].state)
    }
}
