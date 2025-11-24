package pt.isel.http.controllers

import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import pt.isel.domain.Game.Round.RoundState
import pt.isel.domain.Game.pokerDice.Command
import pt.isel.domain.Game.pokerDice.GamePhase
import pt.isel.domain.user.AuthenticatedUser
import pt.isel.http.model.problem.Problem
import pt.isel.service.Auxiliary.Either
import pt.isel.service.matchService.MatchService
import pt.isel.service.matchService.MatchManager
import pt.isel.service.Auxiliary.Failure
import pt.isel.service.Auxiliary.Success
import pt.isel.service.matchService.MatchEventFormatter
import pt.isel.service.matchService.MatchServiceError
import pt.isel.service.matchService.sseMatchService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

@RestController
@RequestMapping("/api/matches/sse")
class SseMatchController(
    private val matchManager: MatchManager,
    private val matchService: MatchService,
    private val eventFormatter: MatchEventFormatter,
    private val sseMatchService: sseMatchService
) {

    /** Mapa que associa cada matchId a uma lista de listeners (funções) que serão notificadas dos eventos.
     *  Cada listener é uma função que recebe três parâmetros:
     * - eventType: String? - o tipo do evento (ou nulo para heartbeats)
     * - eventId: String? - o identificador do evento (ou nulo para heartbeats)
     * - payload: Any? - o payload do evento (ou nulo para heartbeats)
     */
    private val listenersByMatch = mutableMapOf<Int, MutableList<(String?, String?, Any?) -> Unit>>()
    private val lock = ReentrantLock()

    /** Contador para contar cada conexão SSE */
    private var connectionCounter = 0

    // Executor para heartbeats globais
    private val executor = Executors.newScheduledThreadPool(1).also {
        it.scheduleAtFixedRate({ keepAlive() }, 15, 15, TimeUnit.SECONDS)
    }

    private fun keepAlive() = lock.withLock {
        listenersByMatch.forEach { (_, listeners) ->
            listeners.forEach { listener ->
                try {
                    listener(null, null, null) // Envia heartbeat (parâmetros nulos)
                } catch (ex: Exception) {
                }
            }
        }
    }

    private fun addListener(matchId: Int, listener: (String?, String?, Any?) -> Unit) = lock.withLock {
        listenersByMatch.computeIfAbsent(matchId) { mutableListOf() }.add(listener)
    }

    private fun removeListener(matchId: Int, listener: (String?, String?, Any?) -> Unit) = lock.withLock {
        listenersByMatch[matchId]?.remove(listener)
        if (listenersByMatch[matchId]?.isEmpty() == true) {
            listenersByMatch.remove(matchId)
        }
    }

    private fun broadcastEvent(matchId: Int, eventType: String, eventId: String, payload: Any) = lock.withLock {
        val listeners = listenersByMatch[matchId] ?: return
        listeners.forEach { listener ->
            try {
                listener(eventType, eventId, payload)
            } catch (ex: Exception) {
            }
        }
    }

    @GetMapping("/{matchId}/events", produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun listenMatch(@PathVariable matchId: Int): SseEmitter {
        val connId = ++connectionCounter // Incrementa o contador de conexões
        val emitter = SseEmitter(TimeUnit.HOURS.toMillis(1))

        // Envia snapshot com o estado inicial da partida
        matchManager.get(matchId)?.snapshot()?.let { banked ->
            try {
                val eventId = "$matchId"
                val eventType = "match-snapshot"
                val payload = eventFormatter.createEnrichedPayload(banked, eventType, null, eventId)
                emitter.send(SseEmitter.event().name(eventType).id(eventId).data(payload))
            } catch (_: Exception) {}
        }

        // Cria listener para heartbeats e broadcasts
        val listener: (String?, String?, Any?) -> Unit = { eventType, eventId, payload ->
            try {
                if (eventType == null) {
                    // Heartbeat
                    emitter.send(SseEmitter.event().comment("heartbeat"))
                } else {
                    // Evento específico
                    emitter.send(SseEmitter.event().name(eventType).id(eventId).data(payload))
                }
            } catch (ex: Exception) {
                emitter.completeWithError(ex)
            }
        }

        addListener(matchId, listener)

        emitter.onCompletion {
            removeListener(matchId, listener)
        }
        emitter.onTimeout {
            removeListener(matchId, listener)
        }

        return emitter
    }

    @PostMapping("/{matchId}/events", consumes = [MediaType.APPLICATION_JSON_VALUE])
    fun postCommand(
        @PathVariable matchId: Int,
        authenticatedUser: AuthenticatedUser,
        @RequestBody body: Map<String, Any>
    ): ResponseEntity<Any> {
        val rawType = sseMatchService.getRawTypeFromBody(body)
            ?: return Problem.InvalidBodyParameters.response(HttpStatus.BAD_REQUEST)

        val indices: Set<Int> = sseMatchService.getIndicesFromBody(body)
        val userId = authenticatedUser.user.id

        val cmdResult: Either<MatchServiceError, Command> = sseMatchService.executeComand(rawType, userId, indices)

        return when (cmdResult) {
            is Failure -> {
                when (cmdResult.value) {
                    MatchServiceError.CommandUnknown -> Problem.CommandUnknown.response(HttpStatus.BAD_REQUEST)
                    MatchServiceError.CommandInvalidIndices -> Problem.InvalidIndices.response(HttpStatus.BAD_REQUEST)
                    else -> Problem.Unknown.response(HttpStatus.BAD_REQUEST)
                }
            }
            is Success -> {
                val cmd = cmdResult.value
                val prevState = matchManager.get(matchId)?.snapshot() // aqui retorna o estado antes do comando ser aplicado retornando o valor mais recente
                when (val res = matchService.applyCommand(matchId, cmd)) {
                    is Success -> {
                        val afterState = matchManager.get(matchId)?.snapshot()
                        if (afterState != null) {
                            val eventId = "$matchId"

                            val responsePayload = when (cmd) {
                                is Command.Roll -> eventFormatter.createEnrichedPayload(afterState, "dice-rolled", userId, eventId)
                                is Command.Hold -> eventFormatter.createEnrichedPayload(afterState, "dice-held", userId, eventId)
                                else -> mapOf("status" to "command applied")
                            }


                            // quando o jogo termina, o numero de rondas mantém se igual , portanto temos de arranjar um diferencial
                            val roundEndedAndNewStarted = prevState?.game?.rounds?.size != afterState.game.rounds.size
                            val gameState = afterState.game.phase // qual é o estado atual do jogo

                            if (gameState == GamePhase.FINISHED ){
                                // Broadcast fim do jogo
                                val gameEndType = "game-end"
                                val gameEndPayload = eventFormatter.createEnrichedPayload(afterState, gameEndType, userId, eventId)
                                broadcastEvent(matchId, gameEndType, eventId, gameEndPayload)
                            }

                           else if (roundEndedAndNewStarted) {
                                // Broadcast fim de ronda para todos os ouvintes
                                val roundEventType = "round-complete"
                                val roundPayload = eventFormatter.createEnrichedPayload(afterState, roundEventType, userId, eventId)
                                broadcastEvent(matchId, roundEventType, eventId, roundPayload)

                                // Verificar se o jogo terminou (apenas um jogador com saldo >= ante ou fase do jogo é FINISHED)

                                val ante = afterState.game.ante
                                val playersAbleToPay = afterState.wallets.count { it.value.currentBalance >= ante }
                                val shouldEndGame = playersAbleToPay <= 1
                                if (shouldEndGame) {
                                    // Broadcast fim do jogo
                                    val gameEndType = "game-end"
                                    val gameEndPayload = eventFormatter.createEnrichedPayload(afterState, gameEndType, userId, eventId)
                                    broadcastEvent(matchId, gameEndType, eventId, gameEndPayload)
                                } else {
                                    // começa nova ronda
                                    val newEventType = "match-snapshot"
                                    val turnPayload = eventFormatter.createEnrichedPayload(afterState, newEventType, userId, eventId)
                                    broadcastEvent(matchId, newEventType, eventId, turnPayload)
                                }
                            } else if (cmd is Command.FinishTurn || cmd is Command.NextRound) {

                                val turnEventType = "turn-change"
                                val turnPayload = eventFormatter.createEnrichedPayload(afterState, turnEventType, userId, eventId)
                                broadcastEvent(matchId, turnEventType, eventId, turnPayload)
                            }

                            return ResponseEntity.status(HttpStatus.OK).body(responsePayload)
                        }
                        return ResponseEntity.status(HttpStatus.OK).build()
                    }
                    is Failure ->
                        when (res.value) {
                            MatchServiceError.NotYourTurn -> Problem.NotYourTurn.response(HttpStatus.BAD_REQUEST)
                            MatchServiceError.Unknown -> Problem.CommandUnknown.response(HttpStatus.BAD_REQUEST)
                            else -> Problem.Unknown.response(HttpStatus.BAD_REQUEST)
                        }
                }
            }
        }
    }
}