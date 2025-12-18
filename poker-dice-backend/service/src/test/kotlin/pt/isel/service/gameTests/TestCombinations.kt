package pt.isel.service.gameTests

import org.junit.jupiter.api.Test
import pt.isel.domain.Game.Combination
import pt.isel.domain.Game.Face
import pt.isel.domain.Game.Hand
import kotlin.test.assertEquals

class TestCombinations {
    @Test
    fun `testa calculo de Five of a Kind`() {
        val hand = Hand(listOf(Face.ACE, Face.ACE, Face.ACE, Face.ACE, Face.ACE))
        val (combination, _) = Combination.calculate(hand)
        assertEquals(Combination.FIVE_OF_A_KIND, combination)
    }

    @Test
    fun `testa calculo de Full House`() {
        val hand = Hand(listOf(Face.KING, Face.KING, Face.KING, Face.QUEEN, Face.QUEEN))
        val (combination, _) = Combination.calculate(hand)
        assertEquals(Combination.FULL_HOUSE, combination)
    }

    @Test
    fun `testa calculo de Straight`() {
        val hand = Hand(listOf(Face.NINE, Face.TEN, Face.JACK, Face.QUEEN, Face.KING))
        val (combination, _) = Combination.calculate(hand)
        assertEquals(Combination.STRAIGHT, combination)
    }

    @Test
    fun `testa comparacao de maos com combinacoes diferentes`() {
        val hand1 = Hand(listOf(Face.ACE, Face.ACE, Face.ACE, Face.ACE, Face.ACE)) // Five of a Kind
        val hand2 = Hand(listOf(Face.KING, Face.KING, Face.KING, Face.QUEEN, Face.QUEEN)) // Full House
        val result = Combination.compareHands(hand1, hand2)
        assertEquals(hand1, result)
    }

    @Test
    fun `testa calculo de Two-Pair`() {
        val hand = Hand(listOf(Face.NINE, Face.NINE, Face.JACK, Face.JACK, Face.KING))
        val (combination, _) = Combination.calculate(hand)
        assertEquals(Combination.TWO_PAIR, combination)
    }

    @Test
    fun `testa calculo de Straight entre duas mãos (hand1 maior)`() {
        val hand1 = Hand(listOf(Face.NINE, Face.TEN, Face.JACK, Face.QUEEN, Face.KING))
        val hand2 = Hand(listOf(Face.TEN, Face.JACK, Face.QUEEN, Face.KING, Face.ACE))
        val result = Combination.compareHands(hand1, hand2)
        assertEquals(hand2, result)
    }

    @Test
    fun `testa comparacao de maos(two-pair vs two-pair) com mesma combinacao`() {
        val hand1 = Hand(listOf(Face.ACE, Face.ACE, Face.KING, Face.KING, Face.QUEEN)) // Two Pair
        val hand2 = Hand(listOf(Face.KING, Face.KING, Face.QUEEN, Face.QUEEN, Face.JACK)) // Two Pair
        val result = Combination.compareHands(hand1, hand2)
        assertEquals(hand1, result)
    }
}
