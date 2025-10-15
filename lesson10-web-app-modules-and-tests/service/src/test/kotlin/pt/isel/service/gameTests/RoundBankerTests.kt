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
        val ante = Ante(amount = 10, matchId = 77L, roundNumber = 1)
        val wallets = mapOf(
            1L to Wallet(userId = 1L, currentBalance = 100),
            2L to Wallet(userId = 2L, currentBalance = 100)
        )

        val rf = RoundBanker.openPot(ante, playerIds = listOf(1L, 2L), wallets = wallets)

        assertFalse(rf.pot.closed)
        assertEquals(77L, rf.pot.matchId)
        assertEquals(1, rf.pot.roundNumber)
        assertEquals(90, rf.wallets[1L]!!.currentBalance)
        assertEquals(90, rf.wallets[2L]!!.currentBalance)
    }

    @Test
    fun `settleAndPay pays winners and closes the pot`() {
        val ante = Ante(amount = 10, matchId = 99L, roundNumber = 1)
        val initial = mapOf(
            1L to Wallet(1L, 100),
            2L to Wallet(2L, 100)
        )
        val opened = RoundBanker.openPot(ante, listOf(1L, 2L), initial)

        val result = RoundBanker.settleAndPay(
            pot = opened.pot,
            winnerUserIds = setOf(1L),
            wallets = opened.wallets
        )

        assertTrue(result.closedPot.closed)
        // Entire pot (20) goes to user 1
        assertEquals(20, result.payouts[1L])
        assertEquals(110, result.wallets[1L]!!.currentBalance)
        assertEquals(90, result.wallets[2L]!!.currentBalance)
    }

    @Test
    fun `openPot fails if a player's wallet is missing`() {
        val ante = Ante(amount = 5, matchId = 11L, roundNumber = 1)
        val wallets = mapOf(1L to Wallet(1L, 50)) // missing 2L

        assertThrows<IllegalStateException> {
            RoundBanker.openPot(ante, playerIds = listOf(1L, 2L), wallets = wallets)
        }
    }
}
