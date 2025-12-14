package pt.isel.domain.Game.Match

import pt.isel.domain.Game.Face
import pt.isel.domain.Game.Hand
import pt.isel.domain.Game.money.Wallet

sealed class MatchEvent {
    data class TurnChange(
        val previousPlayer: Int,
        val currentPlayer: Int
    ): MatchEvent()

    data class RoundSummary(
        val roundNumber: Int,
        val winners: List<Int> = emptyList(),
        val prize: Int,
        val wallets: Map<Int, Wallet>,
        val playersAndCombinations: Map<Int, RoundHandView> = emptyMap()
    ) : MatchEvent() {

        data class RoundHandView(
            val dices: List<DiceView>,
            val combination: String
        )

        data class DiceView(
            val value: Face

        )
    }

    data class MatchSnapshot(
        val matchId: Int,
        val currentRoundNumber: Int,
        val totalRounds : Int,
        val playerOrder: List<Int>,
        val currentPlayer: Int
    ): MatchEvent()

    data class GameEndPayload(
        val winner: Int,
        val wallets: Map<Int, Wallet>
    ): MatchEvent()


    data class DiceRolled(
        val userId: Int,
        val dice: List<String>,
        val rerollsLeft: Int,
        val combination: String
    ) : MatchEvent()

    data class DiceHeld(
        val userId: Int,
        val heldIndices: List<Int>
    ) : MatchEvent()
}