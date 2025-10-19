
package pt.isel.service.match

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import pt.isel.domain.Game.money.BankedMatch
import pt.isel.domain.Game.money.BankedMatchEngine
import pt.isel.domain.Game.pokerDice.Command

class BankedGameMatchEngine(
    val matchId: Int,
    initial: BankedMatch,
) {
    private val _state = MutableStateFlow(initial)
    val state: StateFlow<BankedMatch> = _state
    private val mutex = Mutex()

    suspend fun dispatch(cmd: Command): Result<Unit> = try {
        mutex.withLock {
            val next = BankedMatchEngine.apply(_state.value, cmd)
            _state.value = next
        }
        Result.success(Unit)
    } catch (t: Throwable) {
        Result.failure(t)
    }

    fun snapshot(): BankedMatch = _state.value
}
