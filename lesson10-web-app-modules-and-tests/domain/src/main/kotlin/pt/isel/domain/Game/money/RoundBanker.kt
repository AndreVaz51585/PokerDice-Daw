package pt.isel.domain.Game.money

/**
 * Orchestrates real-money flows for a match round:
 * - openPot: collects antes from wallets into a Pot (with invariants).
 * - settleAndPay: closes the Pot, splits winnings, and deposits to winner wallets.
 */
object RoundBanker {

    data class RoundFunds(
        val pot: Pot,
        val wallets: Map<Long, Wallet>
    )

    data class PayoutResult(
        val closedPot: Pot,
        val payouts: Map<Long, Int>,
        val wallets: Map<Long, Wallet>
    )

    fun openPot(
        ante: Ante,
        playerIds: List<Long>,
        wallets: Map<Long, Wallet>
    ): RoundFunds {
        var pot = Pot(matchId = ante.matchId, roundNumber = ante.roundNumber)
        val updatedWallets = wallets.toMutableMap()

        for (pid in playerIds) {
            val w = updatedWallets[pid] ?: error("Missing wallet for user $pid")
            val (newPot, newWallet) = ante.collectBet(pid, pot, w)
            pot = newPot
            updatedWallets[pid] = newWallet
        }
        return RoundFunds(pot = pot, wallets = updatedWallets.toMap())
    }

    fun settleAndPay(
        pot: Pot,
        winnerUserIds: Set<Long>,
        wallets: Map<Long, Wallet>
    ): PayoutResult {
        val closed = pot.close()
        val splits = closed.computeWinnerSplits(winnerUserIds)
        val updatedWallets = wallets.toMutableMap()

        for ((uid, amount) in splits) {
            val w = updatedWallets[uid] ?: error("Missing wallet for user $uid")
            updatedWallets[uid] = w.deposit(amount)
        }
        return PayoutResult(
            closedPot = closed,
            payouts = splits,
            wallets = updatedWallets.toMap()
        )
    }
}
