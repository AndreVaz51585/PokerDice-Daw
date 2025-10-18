package pt.isel.domain.Game.money

import java.time.Instant

data class Ante(
    val amount: Int,
    val matchId: Long,
    val roundNumber: Int,
    val createdAt: Instant = Instant.now()
) {

    init {
        require(amount > 0) { "O valor do ante deve ser positivo (foi $amount)" }
        require(matchId > 0L) { "matchId inválido" }
        require(roundNumber > 0) { "roundNumber inválido" }
    }

    // Helper to collect ante from a player's wallet into the pot.
    fun collectBet(userId: Long, pot: Pot, wallet: Wallet): Pair<Pot, Wallet> {
        require(userId > 0L) { "userId inválido" }
        require(!pot.closed) { "Pote encerrado" }
        require(pot.matchId == matchId && pot.roundNumber == roundNumber) {
            "Ante não corresponde ao pote (match/round)"
        }
        require(wallet.userId.toLong() == userId) { "Wallet não corresponde ao user" }

        val updatedWallet = wallet.withdraw(amount)
        val updatedPot = pot.addContribution(userId, amount)
        return updatedPot to updatedWallet
    }
}
