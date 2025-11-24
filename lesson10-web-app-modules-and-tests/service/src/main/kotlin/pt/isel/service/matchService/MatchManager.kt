package pt.isel.service.matchService

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import org.springframework.stereotype.Service
import pt.isel.domain.Game.money.BankedMatch
import pt.isel.domain.Game.pokerDice.Command
import pt.isel.service.match.BankedGameMatchEngine
import java.util.concurrent.ConcurrentHashMap

@Service
class MatchManager {
    // Map que guarda os engines ativos na aplicação associados ao respetivo matchId
    private val engines = ConcurrentHashMap<Int, BankedGameMatchEngine>()

    fun register(engine: BankedGameMatchEngine) {
        engines[engine.matchId] = engine
    }

    fun unregister(matchId: Int) {
        engines.remove(matchId)
    }

    fun get(matchId: Int): BankedGameMatchEngine? = engines[matchId]

}
