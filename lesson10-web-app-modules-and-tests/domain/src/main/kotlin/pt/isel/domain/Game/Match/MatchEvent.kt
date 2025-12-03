package pt.isel.domain.Game.Match

import pt.isel.domain.Game.Hand
import pt.isel.domain.Game.money.Wallet

sealed class MatchEvent {
    data class TurnChange(
        val previousPlayer: Int,
        val currentPlayer: Int
    ): MatchEvent()

    data class RoundSummary(
        val roundNumber: Int,
        val winners: List<Int>?,
        val prize: Int,
        val wallets: Map<Int, Wallet>,
        val playersAndCombinations: Map<Int, Hand>?
    ): MatchEvent()

    data class MatchSnapshot(
        val matchId: Int,
        val currentRoundNumber: Int,
        val playerOrder: List<Int>,
        val currentPlayer: Int
    ): MatchEvent()

    data class GameEndPayload(
        val winner: Int,
        val prize: Int,
        val wallets: Map<Int, Wallet>
    ): MatchEvent()
}