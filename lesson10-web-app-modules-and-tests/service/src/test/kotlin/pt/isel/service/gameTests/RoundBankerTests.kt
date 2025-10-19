package pt.isel.service.gameTests

import pt.isel.domain.Game.money.Ante
import pt.isel.domain.Game.money.RoundBanker
import pt.isel.domain.Game.money.Wallet
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RoundBankerTests {

    @Test
    fun `openPot collects ante from all wallets and keeps pot open`() {
        val ante = Ante(amount = 10, matchId = 77, roundNumber = 1)
        val wallets = mapOf(
            1 to Wallet(userId = 1, currentBalance = 100),
            2 to Wallet(userId = 2, currentBalance = 100)
        )

        val rf = RoundBanker.openPot(ante, playerIds = listOf(1, 2), wallets = wallets)

        assertFalse(rf.pot!!.closed)
        assertEquals(77, rf.pot!!.matchId)
        assertEquals(1, rf.pot!!.roundNumber)
        assertEquals(90, rf.wallets[1]!!.currentBalance)
        assertEquals(90, rf.wallets[2]!!.currentBalance)
    }

    @Test
    fun `settleAndPay pays winners and closes the pot`() {
        val ante = Ante(amount = 10, matchId = 99, roundNumber = 1)
        val initial = mapOf(
            1 to Wallet(1, 100),
            2 to Wallet(2, 100)
        )
        val opened = RoundBanker.openPot(ante, listOf(1, 2), initial)

        val result = RoundBanker.settleAndPay(
            pot = opened.pot!!,
            winnerUserIds = setOf(1),
            wallets = opened.wallets
        )

        assertTrue(result.closedPot.closed)
        // Entire pot (20) goes to user 1
        assertEquals(20, result.payouts[1])
        assertEquals(110, result.wallets[1]!!.currentBalance)
        assertEquals(90, result.wallets[2]!!.currentBalance)
    }

    @Test
    fun `openPot fails if a player's wallet is missing`() {
        val ante = Ante(amount = 5, matchId = 11, roundNumber = 1)
        val wallets = mapOf(1 to Wallet(1, 50)) // missing 2L

        assertThrows<IllegalStateException> {
            RoundBanker.openPot(ante, playerIds = listOf(1, 2), wallets = wallets)
        }
    }

    @Test
    fun `openPot returns null and does not mutate wallets when only one player can pay`() {
        val ante = Ante(amount = 10, matchId = 77, roundNumber = 1)
        // only player 1 can pay
        val wallets = mapOf(
            1 to Wallet(userId = 1, currentBalance = 100),
            2 to Wallet(userId = 2, currentBalance = 5)
        )

        val rf = RoundBanker.openPot(ante, playerIds = listOf(1, 2), wallets = wallets)

        // No pot opened when fewer than 2 players can pay
        assertEquals(rf.pot, null)
        // wallets unchanged
        assertEquals(100, rf.wallets[1]!!.currentBalance)
        assertEquals(5, rf.wallets[2]!!.currentBalance)
        // eligible/excluded reflect reality
        assertEquals(listOf(1), rf.eligiblePlayers)
        assertEquals(listOf(2), rf.excludedPlayers)
    }

    @Test
    fun `openPot excludes insolvent players and opens pot when at least two can pay`() {
        val ante = Ante(amount = 10, matchId = 88, roundNumber = 1)
        // player 3 cannot pay, players 1 and 2 can
        val wallets = mapOf(
            1 to Wallet(userId = 1, currentBalance = 100),
            2 to Wallet(userId = 2, currentBalance = 100),
            3 to Wallet(userId = 3, currentBalance = 0)
        )

        val rf = RoundBanker.openPot(ante, playerIds = listOf(1, 2, 3), wallets = wallets)

        // Pot opened because at least two players could pay
        assertTrue(rf.pot != null)
        // Eligible players paid the ante
        assertEquals(90, rf.wallets[1]!!.currentBalance)
        assertEquals(90, rf.wallets[2]!!.currentBalance)
        // Excluded player's wallet unchanged
        assertEquals(0, rf.wallets[3]!!.currentBalance)
        // lists are coherent
        assertTrue(rf.eligiblePlayers.containsAll(listOf(1, 2)))
        assertTrue(rf.excludedPlayers.contains(3))
    }

    @Test
    fun `openPot reports eligible and excluded players correctly when none can pay`() {
        val ante = Ante(amount = 10, matchId = 99, roundNumber = 1)
        // no one can pay
        val wallets = mapOf(
            1 to Wallet(userId = 1, currentBalance = 0),
            2 to Wallet(userId = 2, currentBalance = 0)
        )

        val rf = RoundBanker.openPot(ante, playerIds = listOf(1, 2), wallets = wallets)

        // No pot opened and wallets unchanged
        assertEquals(rf.pot, null)
        assertEquals(0, rf.wallets[1]!!.currentBalance)
        assertEquals(0, rf.wallets[2]!!.currentBalance)
        // eligible should be empty and excluded should contain both
        assertTrue(rf.eligiblePlayers.isEmpty())
        assertTrue(rf.excludedPlayers.containsAll(listOf(1, 2)))
    }

}
