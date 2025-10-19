package pt.isel.service.gameTests

import pt.isel.domain.Game.Face
import pt.isel.domain.Game.pokerDice.*
import pt.isel.domain.Game.Round.RoundState
import kotlin.test.Test
import kotlin.test.assertEquals

class GameEnginePotSplitTest {

    private val seq = generateSequence {
        listOf(Face.KING, Face.KING, Face.KING, Face.KING, Face.KING)
    }.flatten().iterator()
    private fun roll(): Face = seq.next()

    @Test
    fun pot_split_between_two_equal_five_of_a_kind() {
        var g = Game(9, hostId = 1, ante = 5, maxPlayers = 2, totalRounds = 1, matchId = 9)
        g = GameEngine.apply(g, Command.Join(1), ::roll)
        g = GameEngine.apply(g, Command.Join(2), ::roll)
        g = GameEngine.apply(g, Command.Start(1), ::roll)
        g = GameEngine.apply(g, Command.Roll(1), ::roll)
        g = GameEngine.apply(g, Command.FinishTurn(1), ::roll)
        g = GameEngine.apply(g, Command.Roll(2), ::roll)
        g = GameEngine.apply(g, Command.FinishTurn(2), ::roll)
        assertEquals(RoundState.SCORING, g.rounds.last().state)
        g = GameEngine.apply(g, Command.NextRound(1), ::roll)
        assertEquals(5, g.balances[1])
        assertEquals(5, g.balances[2])
    }
}
