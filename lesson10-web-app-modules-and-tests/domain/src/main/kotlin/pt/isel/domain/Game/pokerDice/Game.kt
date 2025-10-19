package pt.isel.domain.Game.pokerDice

import pt.isel.domain.Game.Lobby.Lobby
import pt.isel.domain.Game.Match.Match
import pt.isel.domain.Game.Round.Round


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
    val id: Int,
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
    val balances: Map<Int, Int> = emptyMap()
) {
    val currentRoundNumber: Int get() = rounds.lastOrNull()?.number ?: 0
    val everyoneDone: Boolean get() = players.values.all { it.done && it.dice.size == 5 }
}


fun createNewGame(
    lobby: Lobby,
    match: Match
): Game {
    var futuresPlayers = lobby.players
    //TODO: Tranformar futuresPlayers do tipo users em  Map<Int, PlayerState>


    val game = Game(
        id = lobby.id,
        matchId = match.id,
        hostId =lobby.lobbyHost,
        ante =lobby.ante,
        maxPlayers = lobby.maxPlayers,
        playerOrder = emptyList() ,     //TODO: Fazer uma função que tire a ordem
        players = emptyMap(),           // TODO
        phase = GamePhase.LOBBY,
        totalRounds = match.totalRounds,
        rounds = match.rounds,
        currentPlayerIndex = 0,         //TODO: Fazer uma função que vá buscar o que tem turn a true
        balances = emptyMap()
    )


    return game
}
