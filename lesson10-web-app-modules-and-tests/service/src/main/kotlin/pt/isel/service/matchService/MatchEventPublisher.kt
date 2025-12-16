package pt.isel.service.matchService

import pt.isel.domain.Game.Match.MatchEvent

interface MatchEventPublisher {
    fun publish(
        matchId: Int,
        event: MatchEvent,
    )

    fun subscribe(matchId: Int, listener: (MatchEvent) -> Unit): () -> Unit // retorna unsubscribe
}
