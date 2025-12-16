package pt.isel.service.gameTests

import pt.isel.domain.Game.money.Ante
import pt.isel.domain.Game.money.Pot
import pt.isel.domain.Game.money.Wallet
import kotlin.test.Test
import kotlin.test.assertFailsWith

class MoneyModelsTest {
    @Test
    fun ante_validation() {
        assertFailsWith<IllegalArgumentException> { Ante(0, matchId = 1, roundNumber = 1) }
        Ante(5, matchId = 1, roundNumber = 1)
    }

    @Test
    fun pot_validation() {
        assertFailsWith<IllegalArgumentException> { Pot(matchId = 0, roundNumber = 1) }
        assertFailsWith<IllegalArgumentException> { Pot(matchId = 1, roundNumber = 0) }
        Pot(matchId = 1, roundNumber = 1, total = 0)
    }

    @Test
    fun wallet_validation() {
        assertFailsWith<IllegalArgumentException> { Wallet(userId = 1, currentBalance = -1) }
        Wallet(userId = 2, currentBalance = 0)
    }
}
