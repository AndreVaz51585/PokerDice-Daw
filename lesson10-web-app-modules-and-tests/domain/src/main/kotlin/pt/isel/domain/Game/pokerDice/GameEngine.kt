package pt.isel.domain.Game.pokerDice

import pt.isel.domain.Game.Face
import pt.isel.domain.Game.Round.Round
import pt.isel.domain.Game.Round.RoundState


/**
 * Applies commands producing a new immutable Game state.
 * All validations use require(...) -> IllegalArgumentException on invalid intent.
 */
object GameEngine {

    fun apply(state: Game, cmd: Command, roll: () -> Face): Game {
        return when (cmd) {

            is Command.Join -> {
                require(state.phase == GamePhase.LOBBY) { "Not in lobby." }
                require(cmd.userId !in state.players) { "Already joined." }
                require(state.players.size < state.maxPlayers) { "Lobby full." }
                state.copy(
                    playerOrder = state.playerOrder + cmd.userId,
                    players = state.players + (cmd.userId to PlayerState(cmd.userId))
                )
            }

            is Command.Start -> {
                require(state.phase == GamePhase.LOBBY) { "Already started." }
                require(cmd.byUserId == state.hostId) { "Only host can start." }
                require(state.playerOrder.size >= 2) { "Need >= 2 players." }
                val initialPot = state.playerOrder.size * state.ante
                val firstRound = Round(
                    number = 1, pot = initialPot, state = RoundState.OPEN,
                    id = TODO(),
                    matchId = TODO(),
                    anteCoins = TODO(),
                    winners = TODO(),
                    hands = TODO()
                )
                state.copy(
                    phase = GamePhase.ROLLING,
                    rounds = listOf(firstRound),
                    balances = state.playerOrder.associateWith { 0 },
                    currentPlayerIndex = 0
                )
            }

            is Command.Hold -> {
                require(state.phase == GamePhase.ROLLING) { "Not rolling phase." }
                val turnPlayer = currentTurnPlayer(state)
                require(cmd.userId == turnPlayer) { "Not your turn." }
                val ps = state.players[cmd.userId] ?: error("Player missing.")
                val valid = cmd.indices.filter { it in 0..4 }.toSet()
                state.copy(players = state.players + (cmd.userId to ps.copy(held = valid)))
            }

            is Command.Roll -> {
                require(state.phase == GamePhase.ROLLING) { "Not rolling phase." }
                val turnPlayer = currentTurnPlayer(state)
                require(cmd.userId == turnPlayer) { "Not your turn." }
                val ps = state.players[cmd.userId] ?: error("Player missing.")
                val isInitial = ps.dice.isEmpty()
                require(isInitial || ps.rerollsLeft > 0) { "No rerolls left." }

                val newDice = (0 until 5).map { i ->
                    if (!isInitial && i in ps.held) ps.dice[i] else roll()
                }
                val newState = ps.copy(
                    dice = newDice,
                    rerollsLeft = if (isInitial) ps.rerollsLeft else ps.rerollsLeft - 1
                )
                state.copy(players = state.players + (cmd.userId to newState))
            }

            is Command.FinishTurn -> {
                require(state.phase == GamePhase.ROLLING) { "Not rolling phase." }
                val turnPlayer = currentTurnPlayer(state)
                require(cmd.userId == turnPlayer) { "Not your turn." }
                val ps = state.players[cmd.userId] ?: error("Player missing.")
                require(ps.dice.size == 5) { "Must roll 5 dice first." }

                val updatedPlayers = state.players + (cmd.userId to ps.copy(done = true))
                val everyoneDone = updatedPlayers.values.all { it.done && it.dice.size == 5 }

                if (!everyoneDone) {
                    // Advance to next unfinished player
                    val nextIdx = nextUndoneIndex(state.playerOrder, updatedPlayers, state.currentPlayerIndex)
                    state.copy(players = updatedPlayers, currentPlayerIndex = nextIdx)
                } else {
                    // Mark round SCORING (showdown deferred to NextRound)
                    val updatedRounds = state.rounds.updateLast { it.copy(state = RoundState.SCORING) }
                    state.copy(players = updatedPlayers, rounds = updatedRounds)
                }
            }

            is Command.NextRound -> {
                require(cmd.byUserId == state.hostId) { "Only host advances." }
                require(state.phase == GamePhase.ROLLING) { "Match not active." }
                val currentRound = state.rounds.lastOrNull() ?: error("No round.")
                require(currentRound.state == RoundState.SCORING) { "Round not ready (need all players done)." }

                // Showdown
                val diceMap = state.players.mapValues { it.value.dice }
                val showdown = Showdown.resolve(diceMap)
                val pot = currentRound.pot
                val winners = showdown.winners
                val share = if (winners.isEmpty()) 0 else pot / winners.size // remainder stays lost
                val newBalances = state.balances.toMutableMap().apply {
                    winners.forEach { this[it] = (this[it] ?: 0) + share }
                }

                val closedRound = currentRound.copy(
                    state = RoundState.CLOSED,
                    winners = winners,
                    hands = TODO() // showdown.hands 
                )

                val isLast = currentRound.number >= state.totalRounds
                return if (isLast) {
                    // Finalize match
                    state.copy(
                        rounds = state.rounds.dropLast(1) + closedRound,
                        balances = newBalances,
                        phase = GamePhase.FINISHED
                    )
                } else {
                    // Prepare next round
                    val nextNumber = currentRound.number + 1
                    val nextPot = state.playerOrder.size * state.ante
                    val nextRound = Round(
                        number = nextNumber, pot = nextPot, state = RoundState.OPEN,
                        id = TODO(),
                        matchId = TODO(),
                        anteCoins = TODO(),
                        winners = TODO(),
                        hands = TODO()
                    )
                    val resetPlayers = state.players.mapValues { (_, ps) ->
                        ps.copy(dice = emptyList(), held = emptySet(), rerollsLeft = 2, done = false)
                    }
                    state.copy(
                        rounds = state.rounds.dropLast(1) + closedRound + nextRound,
                        balances = newBalances,
                        players = resetPlayers,
                        currentPlayerIndex = 0
                    )
                }
            }
        }
    }

    private fun currentTurnPlayer(state: Game): Int =
        state.playerOrder[state.currentPlayerIndex]

    private fun nextUndoneIndex(order: List<Int>, players: Map<Int, PlayerState>, current: Int): Int {
        var idx = current
        repeat(order.size) {
            idx = (idx + 1) % order.size
            if (!players[order[idx]]!!.done) return idx
        }
        return current
    }

    private fun <T> List<T>.updateLast(transform: (T) -> T): List<T> =
        if (isEmpty()) this else dropLast(1) + transform(last())
}
