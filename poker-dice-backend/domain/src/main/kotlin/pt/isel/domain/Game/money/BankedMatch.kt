package pt.isel.domain.Game.money

import pt.isel.domain.Game.pokerDice.Game

/**
 * Aggregate that couples the poker-dice Game state with real-money banking state.
 *
 * - matchId: required for Pot/Ante invariants.
 * - game: current poker-dice game state.
 * - wallets: current wallets by userId (Long).
 * - openPot: the currently open pot for the active round (null only before Start or after settlement).
 */
data class BankedMatch(
    val matchId: Int,
    val game: Game,
    val wallets: Map<Int, Wallet>,
    val openPot: Pot? = null,
)
