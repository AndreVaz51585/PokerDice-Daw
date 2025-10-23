package pt.isel.service.matchService

import org.springframework.stereotype.Service
import pt.isel.domain.Game.Combination
import pt.isel.domain.Game.Hand
import pt.isel.domain.Game.Hand.Companion.getCombination
import pt.isel.domain.Game.Match.MatchState
import pt.isel.domain.Game.Round.RoundState
import pt.isel.domain.Game.money.BankedMatch
import pt.isel.domain.Game.money.Wallet
import pt.isel.domain.Game.pokerDice.Game
import pt.isel.domain.Game.pokerDice.PlayerState
import pt.isel.domain.Game.pokerDice.Showdown
import kotlin.collections.get
import kotlin.rem
import kotlin.text.compareTo
import kotlin.text.equals
import kotlin.text.get
import kotlin.toString


@Service
class MatchEventFormatter {


    // Defina classes de payload minimalistas
    data class RollResultPayload(
        val userId: Int,
        val dice: List<Any>,
        val combination: String,
        val rerollsLeft: Int
    )

    data class HoldResultPayload(
        val userId: Int,
        val dice: List<Any>,
        val heldIndices: List<Int>,

        )

    data class TurnChangePayload(
        val previousPlayer: Int,
        val currentPlayer: Int
    )

    data class RoundSummaryPayload(
        val roundNumber: Int,
        val winners: List<Int>,
        val prize: Int,
        val wallets: Map<Int, Wallet>,
        val playersAndCombinations: Map<Int, Hand>?
    )

    data class SimpleMatchSnapshot(
        val matchId: Int,
        val currentRoundNumber: Int ,
        val playerOrder: List<Int>,
        val currentPlayer: Int
    )

    data class GameEndPayload(
        val winner: Int,
        val prize: Int,
        val wallets: Map<Int, Wallet>)


    fun createEnrichedPayload(state: BankedMatch, eventType: String, actionBy: Int?, eventId: String): Any {
        return when (eventType) {
            "match-snapshot" -> SimpleMatchSnapshot(
                matchId = state.matchId,
                currentRoundNumber = state.game.rounds.size,
                playerOrder = state.game.playerOrder,
                currentPlayer = state.game.playerOrder.getOrNull(state.game.currentPlayerIndex) ?: -1
            )

            "dice-rolled" -> {
                val player = actionBy?.let { state.game.players[it] }
                if (player != null) {
                    RollResultPayload(
                        userId = actionBy,
                        dice = player.dice,
                        combination = Hand(player.dice).getCombination().first.toString(),
                        rerollsLeft = player.rerollsLeft
                    )
                } else mapOf("error" to "Jogador não encontrado")
            }

            "dice-held" -> {
                val player = actionBy?.let { state.game.players[it] }
                if (player != null) {
                    HoldResultPayload(
                        userId = actionBy,
                        dice = player.dice,
                        heldIndices = player.held.toList()
                    )
                } else mapOf("error" to "Jogador não encontrado")
            }

            "turn-change" -> {
                val prevIndex =
                    (state.game.currentPlayerIndex - 1 + state.game.playerOrder.size) % state.game.playerOrder.size
                val prevPlayerId = state.game.playerOrder.getOrNull(prevIndex) ?: -1
                val currPlayerId = state.game.playerOrder.getOrNull(state.game.currentPlayerIndex) ?: -1

                TurnChangePayload(
                    previousPlayer = prevPlayerId,
                    currentPlayer = currPlayerId
                )
            }

            "round-complete" -> {
                val completedRoundIndex = state.game.rounds.size - 2
                val completedRound = state.game.rounds.getOrNull(completedRoundIndex)
                val playersCombinations = completedRound?.hands

                RoundSummaryPayload(
                    roundNumber = completedRound?.number ?: 0,
                    winners = completedRound?.winners ?: emptyList(),
                    prize = completedRound?.pot ?: 0,
                    wallets = state.wallets,
                    playersAndCombinations = playersCombinations
                )
            }

            "game-end" -> {
                val finalWinner = state.wallets.maxByOrNull { it.value.currentBalance }?.key ?: -1
                val finalPrize = state.wallets[finalWinner]?.currentBalance ?: 0

                GameEndPayload(
                    winner = finalWinner,
                    prize = finalPrize,
                    wallets = state.wallets
                )
            }

            else -> mapOf(
                "eventType" to eventType,
                "matchId" to state.matchId
            )
        }
    }


}