package pt.isel.http.controllers

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
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
import pt.isel.domain.Game.money.BankedMatch
import pt.isel.domain.Game.pokerDice.Command
import pt.isel.domain.user.AuthenticatedUser
import pt.isel.http.model.problem.Problem
import pt.isel.service.matchService.MatchService
import pt.isel.service.matchService.MatchManager
import pt.isel.service.Auxiliary.Failure
import pt.isel.service.Auxiliary.Success
import pt.isel.service.matchService.MatchEventFormatter
import pt.isel.service.matchService.MatchServiceError
import kotlin.collections.get

@RestController
@RequestMapping("/api/matches/sse")
class SseMatchController(
    private val matchManager: MatchManager,
    private val matchService: MatchService,
    private val eventFormatter: MatchEventFormatter
) {

    @GetMapping("/{matchId}/events", produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun listenMatch(@PathVariable matchId: Int): SseEmitter {
        val emitter = SseEmitter(0L)
        val job = Job()
        val scope = CoroutineScope(Dispatchers.IO + job)

        // Variáveis para controlo de estado
        var lastState: BankedMatch? = null
        var eventCount = 0

        // Envia snapshot inicial apenas uma vez
        matchManager.get(matchId)?.snapshot()?.let { banked ->
            try {
                val eventId = "$matchId"
                val eventType = "match-snapshot"
                val actionUser = null
                val payload = eventFormatter.createEnrichedPayload(banked, eventType, actionUser, eventId)


                emitter.send(
                    SseEmitter.event()
                        .name(eventType)
                        .id(eventId)
                        .data(payload)
                )
                lastState = banked
                eventCount++
            } catch (_: Exception) { }
        }

        scope.launch {
            try {
                matchManager.events(matchId).collect { banked ->
                    try {
                        // Verifica se é diferente do último estado enviado
                        if (eventFormatter.isDifferentState(lastState, banked)) {
                            val eventId = "$matchId-${eventCount++}"
                            val eventType = eventFormatter.detectEventType(lastState, banked)
                            val actionUser = eventFormatter.detectActionUser(lastState, banked)
                            val payload = eventFormatter.createEnrichedPayload(banked, eventType, actionUser,  eventId)

                            emitter.send(
                                SseEmitter.event()
                                    .name("match-state")
                                    .id(eventId)
                                    .data(payload)
                            )

                            lastState = banked
                        }
                    } catch (t: Throwable) {
                        throw t
                    }
                }
            } catch (t: Throwable) {
                emitter.completeWithError(t)
            }
        }

        emitter.onCompletion { job.cancel() }
        emitter.onTimeout { job.cancel(); emitter.complete() }
        emitter.onError { job.cancel() }

        return emitter
    }


    @PostMapping("/{matchId}/events", consumes = [MediaType.APPLICATION_JSON_VALUE])
    fun postCommand(
        @PathVariable matchId: Int,
        authenticatedUser : AuthenticatedUser,
        @RequestBody body: Map<String, Any>
    ): ResponseEntity<Any> {
        val rawType = (body["type"] ?: body["action"])?.toString()?.lowercase()
            ?: return ResponseEntity.badRequest().body(mapOf("error" to "missing action/type"))

        val indices: Set<Int> = when (val v = body["indices"]) {
            is List<*> -> v.mapNotNull { (it as? Number)?.toInt() }.toSet()
            is Array<*> -> v.mapNotNull { (it as? Number)?.toInt() }.toSet()
            else -> emptySet()
        }

        val userId = authenticatedUser.user.id
        val cmd: Command = when (rawType) {
            "join" -> Command.Join(userId)
            "start" -> Command.Start(byUserId = userId)
            "hold" -> Command.Hold(userId = userId, indices = indices)
            "roll" -> Command.Roll(userId)
            "finishturn", "finish-turn", "finish_turn" -> Command.FinishTurn(userId)
            "nextround", "next-round", "next_round" -> Command.NextRound(byUserId = userId)
            else -> return ResponseEntity.badRequest().body(mapOf("error" to "unknown command type"))
        }

        return when (val res = matchService.applyCommand(matchId, cmd)) {
            is Success -> ResponseEntity.status(HttpStatus.OK).build()
            is Failure ->
                when(res.value) {
                 MatchServiceError.Unknown -> Problem.CommandUnknown.response(HttpStatus.BAD_REQUEST)
                    else -> Problem.Unknown.response(HttpStatus.BAD_REQUEST)
                }
        }
    }
}
