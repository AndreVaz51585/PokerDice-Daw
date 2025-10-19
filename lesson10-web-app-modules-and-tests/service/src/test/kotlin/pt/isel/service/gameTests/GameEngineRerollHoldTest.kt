package pt.isel.service.gameTests

import pt.isel.domain.Game.Face
import pt.isel.domain.Game.pokerDice.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GameEngineRerollHoldTest {

    private val faces = listOf(Face.KING, Face.QUEEN, Face.JACK, Face.TEN, Face.NINE, Face.ACE).iterator()
    private fun roll(): Face = if (faces.hasNext()) faces.next() else Face.ACE

    @Test
    fun hold_preserves_selected_indices() {
        var g = Game(id = 3, hostId = 7, ante = 1, maxPlayers = 2, matchId = 3)
        g = GameEngine.apply(g, Command.Join(7), ::roll)
        g = GameEngine.apply(g, Command.Join(8), ::roll)
        g = GameEngine.apply(g, Command.Start(7), ::roll)

        // First player initial roll
        g = GameEngine.apply(g, Command.Roll(7), ::roll)
        val firstDice = g.players[7]!!.dice
        g = GameEngine.apply(g, Command.Hold(7, setOf(1,3)), ::roll)
        g = GameEngine.apply(g, Command.Roll(7), ::roll) // reroll others
        val after = g.players[7]!!.dice
        assertEquals(firstDice[1], after[1])
        assertEquals(firstDice[3], after[3])
        assertTrue(firstDice[0] != after[0] || firstDice[2] != after[2] || firstDice[4] != after[4])
    }
}
