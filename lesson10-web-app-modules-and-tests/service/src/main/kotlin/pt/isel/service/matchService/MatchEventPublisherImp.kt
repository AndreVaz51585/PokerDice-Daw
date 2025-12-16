package pt.isel.service.matchService

import org.springframework.stereotype.Service
import pt.isel.domain.Game.Match.MatchEvent
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

@Service
class MatchEventPublisherImp : MatchEventPublisher {
    private val listeners = ConcurrentHashMap<Int, MutableList<(MatchEvent) -> Unit>>()
    private val lock = ReentrantLock()

    override fun publish(
        matchId: Int,
        event: MatchEvent,
    ): Unit =
        lock.withLock {
            listeners[matchId]?.forEach { listener ->
                try {
                    listener(event)
                } catch (_: Exception) {
                }
            }
        }

    override fun subscribe(
        matchId: Int,
        listener: (MatchEvent) -> Unit,
    ): () -> Unit =
        lock.withLock {
            listeners.computeIfAbsent(matchId) { mutableListOf() }.add(listener)
            return { unsubscribe(matchId, listener) }
        }

    private fun unsubscribe(
        matchId: Int,
        listener: (MatchEvent) -> Unit,
    ) = lock.withLock {
        listeners[matchId]?.remove(listener)
        if (listeners[matchId]?.isEmpty() == true) listeners.remove(matchId)
    }
}
