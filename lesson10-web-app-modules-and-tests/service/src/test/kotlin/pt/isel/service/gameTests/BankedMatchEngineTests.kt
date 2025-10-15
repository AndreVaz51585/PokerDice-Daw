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

class BankedMatchEngineTests {

    private val roll = { Face.NINE }

    private fun newMatch(
        ante: Int = 10,
        totalRounds: Int = 3
    ): BankedMatch {
        val game = Game(
            id = 1,
            hostId = 10,
            ante = ante,
            maxPlayers = 4,
            totalRounds = totalRounds
        )
        val wallets = mapOf(
            1L to Wallet(userId = 1L, currentBalance = 100),
            2L to Wallet(userId = 2L, currentBalance = 100)
        )
        return BankedMatch(
            matchId = 100L,
            game = game,
            wallets = wallets,
            openPot = null
        )
    }

    private fun joinTwoPlayers(state: BankedMatch): BankedMatch {
        val s1 = BankedMatchEngine.apply(state, Command.Join(1), roll)
        val s2 = BankedMatchEngine.apply(s1, Command.Join(2), roll)
        return s2
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
}
