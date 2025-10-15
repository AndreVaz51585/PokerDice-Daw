package pt.isel.service.matchService

import pt.isel.domain.Game.money.BankedMatch

interface BankedMatchRepository {
    fun load(matchId: Long): BankedMatch?
    fun save(state: BankedMatch)
}

