package pt.isel.domain.Game.money

data class Wallet(
    val userId: Int,
    val currentBalance: Int,
) {
    init {
        require(userId > 0L) { "userId inválido" }
        require(currentBalance >= 0) { "Saldo não pode ser negativo (valor atual: $currentBalance)" }
    }

    fun deposit(amount: Int): Wallet {
        require(amount > 0) { "Depósito deve ser positivo" }
        return copy(
            currentBalance = currentBalance + amount,
        )
    }

    fun withdraw(amount: Int): Wallet {
        require(amount > 0) { "Levantamento deve ser positivo" }
        require(currentBalance >= amount) { "Saldo insuficiente" }
        return copy(
            currentBalance = currentBalance - amount,
        )
    }
}
