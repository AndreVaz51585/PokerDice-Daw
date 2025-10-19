// Kotlin
// file: `lesson10-web-app-modules-and-tests/service/src/main/kotlin/pt/isel/service/events/MatchEventFormatter.kt`
package pt.isel.service.matchService

import org.springframework.stereotype.Service
import pt.isel.domain.Game.Face
import pt.isel.domain.Game.money.BankedMatch
import pt.isel.domain.Game.Round.RoundState

@Service
class MatchEventFormatter {


    data class RoundSummary(val number: Int, val state: String, val winners: List<Int>?)
    data class PlayerDice(val userId: Int, val dice: List<Face>, val held: List<Int>, val rerollsLeft: Int, val done: Boolean)
    data class CompactState(
        val matchId: Int,
        val phase: String,
        val currentPlayerIndex: Int,
        val currentRoundNumber: Int,
        val playerOrder: List<Int>,
        val players: List<PlayerDice>,
        val rounds: List<RoundSummary>,
        val wallets: Map<Int, Int>,
        val openPotClosed: Boolean?,
        val openPotTotal: Int?
    )

    fun toCompactState(state: BankedMatch): CompactState {
        val players = state.game.players.values.map {
            PlayerDice(it.userId, it.dice, it.held.toList(), it.rerollsLeft, it.done)
        }
        val rounds = state.game.rounds.map { r ->
            RoundSummary(r.number, r.state.name, r.winners?.ifEmpty { null })
        }
        val wallets = state.wallets.mapValues { it.value.currentBalance }
        val openPotClosed = state.openPot?.closed
        val openPotTotal = state.openPot?.total
        return CompactState(
            matchId = state.matchId,
            phase = state.game.phase.name,
            currentPlayerIndex = state.game.currentPlayerIndex,
            currentRoundNumber = state.game.currentRoundNumber,
            playerOrder = state.game.playerOrder,
            players = players,
            rounds = rounds,
            wallets = wallets,
            openPotClosed = openPotClosed,
            openPotTotal = openPotTotal
        )
    }

    fun isDifferentState(old: BankedMatch?, new: BankedMatch): Boolean {
        if (old == null) return true
        val o = toCompactState(old)
        val n = toCompactState(new)

        if (o.phase != n.phase) return true
        if (o.currentPlayerIndex != n.currentPlayerIndex) return true
        if (o.currentRoundNumber != n.currentRoundNumber) return true
        if (o.playerOrder != n.playerOrder) return true
        if (o.openPotClosed != n.openPotClosed) return true
        if (o.openPotTotal != n.openPotTotal) return true
        if (o.wallets != n.wallets) return true

        val playersEqual = o.players.size == n.players.size &&
                o.players.zip(n.players).all { (a, b) ->
                    a.userId == b.userId &&
                            a.dice == b.dice &&
                            a.held == b.held &&
                            a.rerollsLeft == b.rerollsLeft &&
                            a.done == b.done
                }
        if (!playersEqual) return true

        val roundsEqual = o.rounds.size == n.rounds.size &&
                o.rounds.zip(n.rounds).all { (a, b) ->
                    a.number == b.number && a.state == b.state && (a.winners ?: emptyList()) == (b.winners)
                }
        if (!roundsEqual) return true

        return false
    }

    fun detectEventType(old: BankedMatch?, new: BankedMatch): String {
        if (old == null) return "match-started"
        if (old.game.currentRoundNumber != new.game.currentRoundNumber) return "round-complete"
        if (old.game.phase != new.game.phase) return "phase-${new.game.phase.name.lowercase()}"
        if (old.game.currentPlayerIndex != new.game.currentPlayerIndex) return "turn-change"
        val curId = getCurrentPlayerId(new) ?: return "state-updated"
        val oldPlayer = old.game.players[curId]
        val newPlayer = new.game.players[curId]
        if (oldPlayer != null && newPlayer != null && oldPlayer.dice != newPlayer.dice) return "dice-rolled"
        if (oldPlayer != null && newPlayer != null && oldPlayer.held != newPlayer.held) return "dice-held"
        return "state-updated"
    }

    fun detectActionUser(old: BankedMatch?, new: BankedMatch): Int? {
        if (old == null) return null
        val curIndex = new.game.currentPlayerIndex
        val prevIndex = old.game.currentPlayerIndex
        if (curIndex != prevIndex) return old.game.playerOrder.getOrNull(prevIndex)
        return new.game.playerOrder.getOrNull(curIndex)
    }

    private fun getCurrentPlayerId(state: BankedMatch): Int? =
        state.game.playerOrder.getOrNull(state.game.currentPlayerIndex)

    fun createEnrichedPayload(
        state: BankedMatch,
        eventType: String,
        actionUserId: Int?,
        eventId: String
    ): Map<String, Any?> {
        val compact = toCompactState(state)
        val winners = state.game.rounds.firstOrNull { it.state == RoundState.CLOSED }?.winners?.ifEmpty { null }
        return mapOf(
            "eventId" to eventId,
            "matchId" to state.matchId,
            "eventType" to eventType,
            "actionBy" to (actionUserId ?: "system"),
            "currentPlayer" to compact.playerOrder.getOrNull(compact.currentPlayerIndex),
            "timestamp" to System.currentTimeMillis(),
            "phase" to compact.phase,
            "currentRoundNumber" to compact.currentRoundNumber,
            "winners" to winners,
            "state" to compact
        )
    }

    fun fingerprint(payload: Any): Int = payload.hashCode()
}
