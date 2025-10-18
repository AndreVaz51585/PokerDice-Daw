package pt.isel.service.gameTests

import pt.isel.domain.Game.money.BankedMatch
import pt.isel.domain.Game.money.BankedMatchEngine
import pt.isel.domain.Game.money.Wallet
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import pt.isel.domain.Game.Face
import pt.isel.domain.Game.pokerDice.Command
import pt.isel.domain.Game.pokerDice.Game
import pt.isel.domain.Game.pokerDice.GamePhase
import kotlin.test.assertTrue

class BankedMatchEngineTests {

    private val roll = { Face.NINE }

    private fun newMatch(
        ante: Int = 10,
        totalRounds: Int = 3,
        wallets: Map<Long, Wallet>? = null
    ): BankedMatch {
        val game = Game(
            id = 1,
            hostId = 10,
            ante = ante,
            maxPlayers = 4,
            totalRounds = totalRounds
        )
        val defaultWallets = mapOf(
            1L to Wallet(userId = 1, currentBalance = 100),
            2L to Wallet(userId = 2, currentBalance = 100)
        )

        return BankedMatch(
            matchId = 100L,
            game = game,
            wallets = wallets?: defaultWallets,
            openPot = null
        )
    }

    private fun joinTwoPlayers(state: BankedMatch): BankedMatch {
        val s1 = BankedMatchEngine.apply(state, Command.Join(1), roll)
        val s2 = BankedMatchEngine.apply(s1, Command.Join(2), roll)
        return s2
    }

    private fun joinThreePlayers(state: BankedMatch): BankedMatch {
        val s1 = BankedMatchEngine.apply(state, Command.Join(1), roll)
        val s2 = BankedMatchEngine.apply(s1, Command.Join(2), roll)
        val s3 = BankedMatchEngine.apply(s2, Command.Join(3), roll)
        return s3
    }

    private fun playOneFullRoundToScoring(state: BankedMatch): BankedMatch {
        // player 1 turn
        val r1 = BankedMatchEngine.apply(state, Command.Roll(1), roll)
        val f1 = BankedMatchEngine.apply(r1, Command.FinishTurn(1), roll)
        // player 2 turn
        val r2 = BankedMatchEngine.apply(f1, Command.Roll(2), roll)
        val f2 = BankedMatchEngine.apply(r2, Command.FinishTurn(2), roll)
        return f2 // round should now be in SCORING
    }

    @Test
    fun `Start opens pot and collects ante from every wallet`() {
        val initial = joinTwoPlayers(newMatch(ante = 10))
        val started = BankedMatchEngine.apply(initial, Command.Start(byUserId = 10), roll)

        // Game advanced to rolling
        assertEquals(GamePhase.ROLLING, started.game.phase)
        assertEquals(1, started.game.rounds.size)
        assertNotNull(started.openPot)
        assertEquals(false, started.openPot!!.closed)

        // Each wallet paid the ante once
        assertEquals(90, started.wallets[1L]!!.currentBalance)
        assertEquals(90, started.wallets[2L]!!.currentBalance)
    }

    @Test
    fun `NextRound settles previous pot and opens the next one when match continues`() {
        val initial = joinTwoPlayers(newMatch(ante = 10, totalRounds = 3))
        val started = BankedMatchEngine.apply(initial, Command.Start(byUserId = 10), roll)
        val scoring = playOneFullRoundToScoring(started)

        val afterNext = BankedMatchEngine.apply(scoring, Command.NextRound(byUserId = 10), roll)

        // Match continues, open pot for round 2 exists
        assertEquals(GamePhase.ROLLING, afterNext.game.phase)
        assertNotNull(afterNext.openPot)
        // After settlement, a new ante was collected for round 2
        // With deterministic tie, each gets pot share, then pays next ante -> back to 90 each
        assertEquals(90, afterNext.wallets[1L]!!.currentBalance)
        assertEquals(90, afterNext.wallets[2L]!!.currentBalance)
        // Round number advanced
        assertEquals(2, afterNext.game.rounds.last().number)
    }

    @Test
    fun `NextRound on last round settles and clears open pot when match finishes`() {
        val initial = joinTwoPlayers(newMatch(ante = 10, totalRounds = 2))
        val started = BankedMatchEngine.apply(initial, Command.Start(byUserId = 10), roll)

        // Round 1 -> SCORING
        val scoring1 = playOneFullRoundToScoring(started)
        // Settle R1 and open R2
        val afterNext1 = BankedMatchEngine.apply(scoring1, Command.NextRound(byUserId = 10), roll)

        // Round 2 -> SCORING
        val scoring2 = playOneFullRoundToScoring(afterNext1)
        // Settle R2 and finish match (no new pot)
        val finished = BankedMatchEngine.apply(scoring2, Command.NextRound(byUserId = 10), roll)

        assertEquals(GamePhase.FINISHED, finished.game.phase)
        assertNull(finished.openPot)
        // Balances are deterministic and consistent
        // With two players and constant rolls, both typically tie each round.
        // After final settlement, no new ante is collected, so both end at 100.
        assertEquals(100, finished.wallets[1L]!!.currentBalance)
        assertEquals(100, finished.wallets[2L]!!.currentBalance)
    }


    @Test
    fun `Start finishes match and does not open pot when only one player can pay ante`() {
        // player 2 has insufficient balance
        val wallets = mapOf(
            1L to Wallet(userId = 1, currentBalance = 100),
            2L to Wallet(userId = 2, currentBalance = 0)
        )
        val initial = joinTwoPlayers(newMatch(ante = 10, wallets = wallets))
        val started = BankedMatchEngine.apply(initial, Command.Start(byUserId = 10), roll)

        // Since only player1 can pay, the engine should NOT open a pot and should finish
        assertEquals(GamePhase.FINISHED, started.game.phase)
        assertNull(started.openPot)
        // Wallets must remain unchanged (no ante collected)
        assertEquals(100, started.wallets[1L]!!.currentBalance)
        assertEquals(0, started.wallets[2L]!!.currentBalance)
    }

    @Test
    fun `Start excludes players who cannot pay when at least two remain eligible`() {
        // three players: player3 cannot pay
        val wallets = mapOf(
            1L to Wallet(userId = 1, currentBalance = 100),
            2L to Wallet(userId = 2, currentBalance = 100),
            3L to Wallet(userId = 3, currentBalance = 0)
        )
        val initial = joinThreePlayers(newMatch(ante = 10, wallets = wallets))
        val started = BankedMatchEngine.apply(initial, Command.Start(byUserId = 10), roll)

        // Since players 1 and 2 can pay, a pot should be opened and player 3 excluded
        assertEquals(GamePhase.ROLLING, started.game.phase)
        assertNotNull(started.openPot)
        // Player 3 must not be in the playerOrder anymore
        assertTrue(started.game.playerOrder.none { it == 3 })
        // Eligible players have paid the ante
        assertEquals(90, started.wallets[1L]!!.currentBalance)
        assertEquals(90, started.wallets[2L]!!.currentBalance)
        // Excluded player's wallet unchanged (since he didn't pay)
        assertEquals(0, started.wallets[3L]!!.currentBalance)
    }
}

