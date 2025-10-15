package pt.isel.service.matchService


import jakarta.inject.Named
import pt.isel.domain.Game.Face
import pt.isel.domain.Game.money.BankedMatch
import pt.isel.domain.Game.money.BankedMatchEngine
import pt.isel.domain.Game.pokerDice.Command
import pt.isel.repo.TransactionManager

@Named
class BankedMatchService(
    private val repo: BankedMatchRepository,
    private val trx: TransactionManager,
    private val roll: () -> Face // bind to Domain Dice::roll in prod; inject deterministic in tests
) {
    fun handle(matchId: Long, cmd: Command): BankedMatch = trx.run {
        val current = repo.load(matchId) ?: error("Match $matchId not found")
        val updated = BankedMatchEngine.apply(current, cmd, roll)
        repo.save(updated)
        updated
    }
}


