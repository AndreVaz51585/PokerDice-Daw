package pt.isel.service.lobbyService.Lobby

import org.springframework.stereotype.Service
import pt.isel.domain.Game.Lobby.LobbyEvent
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

@Service
class LobbyEventPublisherImpl : LobbyEventPublisher {
    private val listeners = ConcurrentHashMap<Int, MutableList<(LobbyEvent) -> Unit>>()
    private val lock = ReentrantLock()

    override fun publish(
        lobbyId: Int,
        event: LobbyEvent,
    ): Unit =
        lock.withLock {
            listeners[lobbyId]?.forEach { listener ->
                try {
                    listener(event)
                } catch (_: Exception) {
                }
            }
        }

    override fun subscribe(
        lobbyId: Int,
        listener: (LobbyEvent) -> Unit,
    ): () -> Unit =
        lock.withLock {
            listeners.computeIfAbsent(lobbyId) { mutableListOf() }.add(listener)
            return { unsubscribe(lobbyId, listener) }
        }

    private fun unsubscribe(
        lobbyId: Int,
        listener: (LobbyEvent) -> Unit,
    ) = lock.withLock {
        listeners[lobbyId]?.remove(listener)
        if (listeners[lobbyId]?.isEmpty() == true) listeners.remove(lobbyId)
    }
}
