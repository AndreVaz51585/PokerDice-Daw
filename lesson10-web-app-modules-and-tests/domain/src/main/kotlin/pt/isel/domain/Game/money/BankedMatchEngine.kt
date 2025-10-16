package pt.isel.domain.Game.money

import pt.isel.domain.Game.Round.RoundState
import pt.isel.domain.Game.pokerDice.Command
import pt.isel.domain.Game.pokerDice.GameEngine
import pt.isel.domain.Game.pokerDice.GamePhase
import pt.isel.domain.Game.Face

/**
 * Wraps the poker-dice GameEngine and wires money flows with RoundBanker.
 *
 * Behavior:
 * - Start: delegates to GameEngine, then opens a Pot and collects antes from wallets.
 * - NextRound: delegates to GameEngine, settles the just-finished round's Pot paying wallets,
 *              and opens the next round’s Pot if the match continues.
 * - Other commands: pure delegation.
 *
 * Notes:
 * - Uses Int userIds from the game, converted to Long for banking.
 * - Enforces all money invariants via Wallet/Ante/Pot.
 */
object BankedMatchEngine {

    fun apply(state: BankedMatch, cmd: Command, roll: () -> Face): BankedMatch {
        return when (cmd) {
            is Command.Start -> {
                // 1) Run the game transition.
                val newGame = GameEngine.apply(state.game, cmd, roll)

                // 2) Open pot and collect antes for round 1.
                val roundNumber = 1
                val ante = Ante(
                    amount = newGame.ante,
                    matchId = state.matchId,
                    roundNumber = roundNumber
                )
                val playerIds = newGame.playerOrder.map { it.toLong() }
                val opened = RoundBanker.openPot(ante, playerIds, state.wallets)

                state.copy(
                    game = newGame,
                    wallets = opened.wallets,
                    openPot = opened.pot
                )
            }

            is Command.NextRound -> {
                // 1) Run the showdown/advance logic in the game.
                val newGame = GameEngine.apply(state.game, cmd, roll)

                // Determine which round just got closed.
                val rounds = newGame.rounds
                require(rounds.isNotEmpty()) { "No rounds found." }

                val justClosedIndex =
                    if (newGame.phase == GamePhase.FINISHED) rounds.lastIndex
                    else rounds.lastIndex - 1
                require(justClosedIndex >= 0) { "Closed round not found." }

                val closedRound = rounds[justClosedIndex]
                require(closedRound.state == RoundState.CLOSED) { "Expected CLOSED round for settlement." }

                // 2) Settle and pay wallets using winners and the current open pot.
                val currentPot = state.openPot ?: error("No open pot to settle.")
                val winnersLong = closedRound.winners?.map { it.toLong() }?.toSet() ?: emptySet()
                val payout = RoundBanker.settleAndPay(
                    pot = currentPot,
                    winnerUserIds = winnersLong,
                    wallets = state.wallets
                )

                // 3) If match continues, open next round's pot; else finish with no open pot.
                if (newGame.phase == GamePhase.FINISHED) {
                    state.copy(
                        game = newGame,
                        wallets = payout.wallets,
                        openPot = null
                    )
                } else {
                    val nextRound = rounds.last()
                    require(nextRound.state == RoundState.OPEN) { "Expected next round to be OPEN." }

                    val ante = Ante(
                        amount = newGame.ante,
                        matchId = state.matchId,
                        roundNumber = nextRound.number
                    )
                    val playerIds = newGame.playerOrder.map { it.toLong() }
                    val opened = RoundBanker.openPot(ante, playerIds, payout.wallets)

                    state.copy(
                        game = newGame,
                        wallets = opened.wallets,
                        openPot = opened.pot
                    )
                }
            }

            else -> {
                // Pure delegation for all other commands.
                val newGame = GameEngine.apply(state.game, cmd, roll)
                state.copy(game = newGame)
            }
        }
    }
}
