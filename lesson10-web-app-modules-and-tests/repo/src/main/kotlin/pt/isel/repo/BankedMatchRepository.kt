package pt.isel.repo

import pt.isel.domain.Game.money.BankedMatch

interface BankedMatchRepository {
    fun load(matchId: Long): BankedMatch?
    fun save(state: BankedMatch)
}