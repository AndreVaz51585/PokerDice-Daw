package pt.isel.http.controllers

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.Executors.newSingleThreadScheduledExecutor
import java.util.concurrent.TimeUnit

/**
 * Configuração para gerenciar heartbeats SSE.
 * Retorna executor e task para cleanup posterior.
 */
data class HeartbeatConfig(
    val executor: ScheduledExecutorService,
    val task: ScheduledFuture<*>
) {
    fun shutdown() {
        task.cancel(true)
        executor.shutdown()
    }
}

/**
 * Configura heartbeat automático para um SseEmitter.
 * Envia comentários SSE a cada [intervalSeconds] segundos.
 *
 * @param emitter SseEmitter que receberá os heartbeats
 * @param initialDelaySeconds Delay antes do primeiro heartbeat
 * @param intervalSeconds Intervalo entre heartbeats
 * @return HeartbeatConfig para cleanup
 */
fun setupHeartbeat(
    emitter: SseEmitter,
    initialDelaySeconds: Long = 10,
    intervalSeconds: Long = 10
): HeartbeatConfig {
    val executor = newSingleThreadScheduledExecutor()

    val task = executor.scheduleAtFixedRate(
        {
            try {
                emitter.send(SseEmitter.event().comment("heartbeat"))
            } catch (ex: Exception) {
                executor.shutdown()
            }
        },
        initialDelaySeconds,
        intervalSeconds,
        TimeUnit.SECONDS
    )

    return HeartbeatConfig(executor, task)
}
