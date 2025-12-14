package pt.isel.service.match

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import pt.isel.domain.Game.Combination
import pt.isel.domain.Game.Hand
import pt.isel.domain.Game.Hand.Companion.getCombination
import pt.isel.domain.Game.money.BankedMatch
import pt.isel.domain.Game.money.BankedMatchEngine
import pt.isel.domain.Game.pokerDice.Command
import pt.isel.domain.Game.pokerDice.Showdown
import pt.isel.service.statisticsService.StatisticsService

class BankedGameMatchEngine(
    val matchId: Int,
    initial: BankedMatch,
    private val statisticsService: StatisticsService
) {
    private val _state = MutableStateFlow(initial)
    val state: StateFlow<BankedMatch> = _state
    private val mutex = Mutex()

    suspend fun dispatch(cmd: Command): Result<Unit> = try {
        mutex.withLock {
            val current = _state.value
            val next = BankedMatchEngine.apply(_state.value, cmd)
            if (cmd is Command.FinishTurn){
                val dices = current.game.players[cmd.userId]?.dice ?: emptyList()

                val combination = Hand(dices).getCombination().first
                when (combination) {
                    Combination.FIVE_OF_A_KIND -> statisticsService.incrementFiveOfAKind(cmd.userId)
                    Combination.FOUR_OF_A_KIND -> statisticsService.incrementFourOfAKind(cmd.userId)
                    Combination.FULL_HOUSE -> statisticsService.incrementFullHouse(cmd.userId)
                    Combination.STRAIGHT -> statisticsService.incrementStraight(cmd.userId)
                    Combination.THREE_OF_A_KIND -> statisticsService.incrementThreeOfAKind(cmd.userId)
                    Combination.TWO_PAIR -> statisticsService.incrementTwoPair(cmd.userId)
                    Combination.PAIR -> statisticsService.incrementOnePair(cmd.userId)
                    Combination.BUST -> statisticsService.incrementBust(cmd.userId)
                }
            }
            _state.value = next
        }
        Result.success(Unit)
    } catch (t: Throwable) {
        Result.failure(t)
    }

    fun snapshot(): BankedMatch = _state.value
}