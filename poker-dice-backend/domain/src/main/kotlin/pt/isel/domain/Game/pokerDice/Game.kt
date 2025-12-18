package pt.isel.domain.Game.pokerDice

import pt.isel.domain.Game.Lobby.Lobby
import pt.isel.domain.Game.Match.Match
import pt.isel.domain.Game.Match.MatchPlayer
import pt.isel.domain.Game.Round.Round
import kotlin.random.Random.Default.nextInt

/**
 * Aggregate root for a Poker Dice match.
 * - ante: fixed per-round ante each player contributes (pot = players * ante).
 * - playerOrder: fixed turn order decided by join order.
 * - players: current transient states.
 * - rounds: history; last entry is the active round (OPEN or SCORING).
 * - currentPlayerIndex: whose turn (index into playerOrder) while round OPEN.
 * - balances: cumulative winnings (chips) per playerId (does not subtract antes here).
 */
data class Game(
    val matchId: Int,
    val hostId: Int,
    val ante: Int,
    val maxPlayers: Int,
    val playerOrder: List<Int> = emptyList(),
    val players: Map<Int, PlayerState> = emptyMap(),
    val phase: GamePhase = GamePhase.LOBBY,
    val totalRounds: Int = 3,
    val rounds: List<Round> = emptyList(),
    val currentPlayerIndex: Int = 0,
    val balances: Map<Int, Int> = emptyMap(),
) {
    val currentRoundNumber: Int get() = rounds.lastOrNull()?.number ?: 0
    val everyoneDone: Boolean get() = players.values.all { it.done && it.dice.size == 5 }
}

fun createNewGame(
    lobby: Lobby,
    match: Match,
    matchPlayers: List<MatchPlayer>,
): Game {
    // Define player order based on lobby players
    val playerOrder = matchPlayers.map { it.userId }

    // Map player IDs to their initial PlayerState
    val playersMap: Map<Int, PlayerState> =
        playerOrder.associateWith { PlayerState(it) }

    // For each player , asscociate their starting balance
    val balancesMap: Map<Int, Int> =
        playerOrder.associateWith { id ->
            matchPlayers.find { it.userId == id }?.balanceAtStart ?: 0
        }

    val currentIndex = nextInt(playerOrder.size)

    val game =
        Game(
            matchId = match.id,
            hostId = lobby.lobbyHost,
            ante = lobby.ante,
            maxPlayers = lobby.maxPlayers,
            playerOrder = playerOrder,
            players = playersMap,
            phase = GamePhase.LOBBY,
            totalRounds = match.totalRounds,
            rounds = match.rounds,
            currentPlayerIndex = currentIndex,
            balances = balancesMap,
        )

    return game
}
