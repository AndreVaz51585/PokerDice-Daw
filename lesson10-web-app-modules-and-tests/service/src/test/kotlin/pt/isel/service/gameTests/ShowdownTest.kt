package pt.isel.service.gameTests

import pt.isel.domain.Game.Face
import pt.isel.domain.Game.pokerDice.Showdown
import kotlin.test.Test
import kotlin.test.assertEquals

class ShowdownTest {

    @Test
    fun single_winner_four_of_a_kind_vs_full_house() {
        val dice = mapOf(
            1 to listOf(Face.KING, Face.KING, Face.KING, Face.KING, Face.ACE),
            2 to listOf(Face.QUEEN, Face.QUEEN, Face.QUEEN, Face.JACK, Face.JACK)
        )
        val r = Showdown.resolve(dice)
        assertEquals(listOf(1), r.winners)
    }

    @Test
    fun split_pot_identical_hands() {
        val dice = mapOf(
            1 to listOf(Face.ACE, Face.ACE, Face.KING, Face.KING, Face.QUEEN),
            2 to listOf(Face.ACE, Face.ACE, Face.KING, Face.KING, Face.QUEEN)
        )
        val r = Showdown.resolve(dice)
        assertEquals(listOf(1,2), r.winners)
    }
}
