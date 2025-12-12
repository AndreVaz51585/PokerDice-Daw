package pt.isel.http.controllers

import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import pt.isel.domain.Game.Hand
import pt.isel.domain.Game.Hand.Companion.getCombination
import pt.isel.domain.Game.Match.MatchEvent
import pt.isel.domain.Game.Match.responses.DiceHold
import pt.isel.domain.Game.Match.responses.DiceRoll
import pt.isel.domain.Game.pokerDice.Command
import pt.isel.domain.user.AuthenticatedUser
import pt.isel.http.model.problem.Problem
import pt.isel.service.Auxiliary.*
import pt.isel.service.matchService.*
import java.util.concurrent.TimeUnit

@RestController
@RequestMapping("/api/matches")
class MatchController(
    private val matchService: MatchService,
    private val matchManager: MatchManager,
    private val sseMatchService: sseMatchService
) {

    @GetMapping("/{matchId}/events", produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun listenMatch(@PathVariable matchId: Int): SseEmitter {
        val emitter = SseEmitter(TimeUnit.HOURS.toMillis(1))

        // Heartbeat como no LobbyController
        val heartBeat = setupHeartbeat(emitter)

        // Snapshot inicial, se já houver engine
        matchManager.get(matchId)?.snapshot()?.let { banked ->
            try {
                val snapshot = MatchEvent.MatchSnapshot(
                    matchId = banked.matchId,
                    currentRoundNumber = banked.game.rounds.size,
                    totalRounds = banked.game.totalRounds,
                    playerOrder = banked.game.playerOrder,
                    currentPlayer = banked.game.playerOrder[banked.game.currentPlayerIndex],
                )
                emitter.send(SseEmitter.event().name("match-snapshot").id("$matchId").data(snapshot))
            } catch (ex: Exception) {
                emitter.completeWithError(ex)
            }
        }

        // Reencaminhar TODOS os eventos relevantes, incluindo dados
        val unsubscribe = matchService.getEventPublisher().subscribe(matchId) { event ->
            try {
                when (event) {
                    is MatchEvent.MatchSnapshot -> emitter.send(SseEmitter.event().name("match-snapshot").data(event))
                    is MatchEvent.TurnChange     -> emitter.send(SseEmitter.event().name("turn-change").data(event))
                    is MatchEvent.RoundSummary   -> emitter.send(SseEmitter.event().name("round-complete").data(event))
                    is MatchEvent.GameEndPayload -> emitter.send(SseEmitter.event().name("game-end").data(event))
                    is MatchEvent.DiceRolled     -> emitter.send(SseEmitter.event().name("dice-rolled").data(event))
                    is MatchEvent.DiceHeld       -> emitter.send(SseEmitter.event().name("dice-held").data(event))
                    else -> { /* ignore */ }
                }
            } catch (ex: Exception) {
                emitter.completeWithError(ex)
            }
        }

        emitter.onCompletion { unsubscribe(); heartBeat.shutdown() }
        emitter.onTimeout    { unsubscribe(); heartBeat.shutdown() }
        emitter.onError      { unsubscribe(); heartBeat.shutdown() }

        return emitter
    }


    data class CommandRequest(
        val type: String,
        val indices: List<Int>? = emptyList()
    )

    @PostMapping("/{matchId}/commands", consumes = [MediaType.APPLICATION_JSON_VALUE])
    fun executeCommand(
        @PathVariable matchId: Int,
        authenticatedUser: AuthenticatedUser,
        @RequestBody body: CommandRequest
    ): ResponseEntity<*> {
        val rawType = body.type.lowercase()
        val indices = body.indices?.toSet() ?: emptySet()
        val userId = authenticatedUser.user.id

        val cmdResult = sseMatchService.executeComand(rawType, userId, indices)
        if (cmdResult is Failure) {
            return when (cmdResult.value) {
                MatchServiceError.CommandUnknown -> Problem.CommandUnknown.response(HttpStatus.BAD_REQUEST)
                MatchServiceError.CommandInvalidIndices -> Problem.InvalidIndices.response(HttpStatus.BAD_REQUEST)
                else -> Problem.Unknown.response(HttpStatus.BAD_REQUEST)
            }
        }

        val cmd = (cmdResult as Success).value

        return when (val result = matchService.applyCommand(matchId, cmd)) {
            is Success -> {
                val banked = result.value
                val player = banked.game.players[userId]
                ResponseEntity.ok(
                    when (cmd) {
                        is Command.Roll -> DiceRoll(
                            dices = player?.dice,
                            rerollsLeft = player!!.rerollsLeft,
                            hand = Hand(player.dice).getCombination().first.toString()
                        )
                        is Command.Hold -> DiceHold(
                            dices = player?.dice,
                            heldIndices = player!!.held,
                            rerollsLeft = player.rerollsLeft,
                        )
                        is Command.FinishTurn -> mapOf("status" to "command executed successfully")
                        else -> mapOf("status" to "ok")
                    }
                )
            }
            is Failure -> when (result.value) {
                MatchServiceError.NotYourTurn -> Problem.NotYourTurn.response(HttpStatus.BAD_REQUEST)
                else -> Problem.Unknown.response(HttpStatus.BAD_REQUEST)
            }
        }
    }

    @GetMapping("/{matchId}")
    fun getMatch(@PathVariable matchId: Int): ResponseEntity<*> {
        return when (val result = matchService.getMatch(matchId)) {
            is Success -> ResponseEntity.ok(result.value)
            is Failure -> Problem.MatchNotFound.response(HttpStatus.NOT_FOUND)
        }
    }
}