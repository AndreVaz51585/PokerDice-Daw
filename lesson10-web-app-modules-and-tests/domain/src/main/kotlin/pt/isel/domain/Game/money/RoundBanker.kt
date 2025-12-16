package pt.isel.domain.Game.money

/**
 * Orchestrates real-money flows for a match round:
 * - openPot: collects antes from wallets into a Pot (with invariants).
 * - settleAndPay: closes the Pot, splits winnings, and deposits to winner wallets.
 */
object RoundBanker {
    data class RoundFunds(
        val pot: Pot?,
        val wallets: Map<Int, Wallet>,
        val eligiblePlayers: List<Int>,
        val excludedPlayers: List<Int>,
    )

    data class PayoutResult(
        val closedPot: Pot,
        val payouts: Map<Int, Int>,
        val wallets: Map<Int, Wallet>,
    )

    fun openPot(
        ante: Ante,
        playerIds: List<Int>,
        wallets: Map<Int, Wallet>,
    ): RoundFunds {
        // eligible players
        val eligible =
            playerIds.filter { pid ->
                val w = wallets[pid] ?: error("Missing money for user $pid")
                w.currentBalance >= ante.amount
            }
        val excluded = playerIds.filter { it !in eligible }

        // If 0 or 1 eligible players -> do NOT collect antes; return wallets unchanged
        if (eligible.size <= 1) {
            return RoundFunds(
                pot = null, // no pot opened
                wallets = wallets, // unchanged
                eligiblePlayers = eligible,
                excludedPlayers = excluded,
            )
        }

        // Otherwise collect ante from eligible players and build pot
        var pot = Pot(matchId = ante.matchId, roundNumber = ante.roundNumber)
        val updatedWallets = wallets.toMutableMap()

        for (pid in eligible) {
            val w = updatedWallets[pid] ?: error("Missing wallet for user $pid")
            val (newPot, newWallet) = ante.collectBet(pid, pot, w)
            pot = newPot
            updatedWallets[pid] = newWallet
        }

        return RoundFunds(
            pot = pot,
            wallets = updatedWallets.toMap(),
            eligiblePlayers = eligible,
            excludedPlayers = excluded,
        )
    }

    fun settleAndPay(
        pot: Pot,
        winnerUserIds: Set<Int>,
        wallets: Map<Int, Wallet>,
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
            wallets = updatedWallets.toMap(),
        )
    }
}
