package pt.isel.service.matchService

import org.springframework.stereotype.Service
import pt.isel.domain.Game.Combination
import pt.isel.domain.Game.Hand
import pt.isel.domain.Game.Hand.Companion.getCombination
import pt.isel.domain.Game.money.BankedMatch
import pt.isel.domain.Game.money.Wallet
import pt.isel.service.statisticsService.StatisticsService
import pt.isel.service.walletService.WalletService


@Service
class MatchEventFormatter
    (
    private val walletService: WalletService,
    private val statisticsService: StatisticsService
) {

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
        val currentRoundNumber: Int,
        val playerOrder: List<Int>,
        val currentPlayer: Int
    )

    data class GameEndPayload(
        val winner: Int,
        val prize: Int,
        val wallets: Map<Int, Wallet>
    )


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

                val dices = state.game.players[prevPlayerId]?.dice ?: emptyList()
                val combination = Hand(dices).getCombination().first
                when (combination) {
                    Combination.FIVE_OF_A_KIND -> statisticsService.incrementFiveOfAKind(prevPlayerId)
                    Combination.FOUR_OF_A_KIND -> statisticsService.incrementFourOfAKind(prevPlayerId)
                    Combination.FULL_HOUSE -> statisticsService.incrementFullHouse(prevPlayerId)
                    Combination.STRAIGHT -> statisticsService.incrementStraight(prevPlayerId)
                    Combination.THREE_OF_A_KIND -> statisticsService.incrementThreeOfAKind(prevPlayerId)
                    Combination.TWO_PAIR -> statisticsService.incrementTwoPair(prevPlayerId)
                    Combination.PAIR -> statisticsService.incrementOnePair(prevPlayerId)
                    Combination.BUST -> statisticsService.incrementBust(prevPlayerId)
                }
                TurnChangePayload(
                    previousPlayer = prevPlayerId,
                    currentPlayer = currPlayerId
                )
            }

            /*"round-complete" -> {
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
            }*/

            "round-complete" -> {
                val completedRoundIndex = state.game.rounds.size - 2
                val completedRound = state.game.rounds.getOrNull(completedRoundIndex) ?: return RoundSummaryPayload(
                    roundNumber = 0,
                    winners = emptyList(),
                    prize = 0,
                    wallets = state.wallets,
                    playersAndCombinations = null
                )

                val prize = completedRound.pot
                val winners = completedRound.winners ?: emptyList()

                winners.forEach { winnerId ->
                    val wallet = state.wallets[winnerId]
                        ?: error("Wallet não encontrada para userId $winnerId — isto não deveria acontecer")

                    walletService.update(Wallet(wallet.userId, wallet.currentBalance + prize))
                    statisticsService.incrementGamesWon(winnerId)
                }

                RoundSummaryPayload(
                    roundNumber = completedRound.number,
                    winners = winners,
                    prize = prize,
                    wallets = state.wallets,
                    playersAndCombinations = completedRound.hands
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