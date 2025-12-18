package pt.isel.service.lobbyService.GlobalLobby

import org.springframework.stereotype.Service
import pt.isel.domain.Game.GlobalLobby.GlobalLobbyEvent
import java.util.concurrent.CopyOnWriteArrayList

@Service
class GlobalLobbyEventPublisherImpl : GlobalLobbyEventPublisher {
    private val listeners =
        CopyOnWriteArrayList<
            (
                GlobalLobbyEvent,
            ) -> Unit,
            >() // escolha de estrutura thread-safe , sempre que for feito um add() ou set() é criada uma nova cópia da lista

    // garantindo thead-safety  na iteração , poderiamos utilizar tambem uma mutableList com lock
    override fun publish(event: GlobalLobbyEvent) {
        listeners.forEach { it(event) }
    }

    override fun subscribe(listener: (GlobalLobbyEvent) -> Unit): () -> Unit {
        listeners.add(listener)
        return { listeners.remove(listener) }
    }
}
