package pt.isel.repo

import pt.isel.domain.Game.money.Wallet
import pt.isel.domain.Game.money.WalletTransaction

interface RepositoryWallet : Repository<Wallet> {

    // Adiciona uma transação à carteira (crédito ou débito)
    fun addTransaction(
        userId: Int,
        amount: Int,
        roundId: Int? = null
    ): Int  // Retorna ID da transação

    // Obtém o saldo atual (soma de todas as transações) para um usuário
    fun getBalance(userId: Int): Int

    // Obtém o histórico de transações de um usuário
    fun getTransactionHistory(userId: Int): List<WalletTransaction>

    // Obtém transações de um round específico
    fun getTransactionsByRound(userId: Int, roundId: Int): List<WalletTransaction>

    // Verifica se o usuário tem saldo suficiente para uma operação
    fun hasSufficientFunds(userId: Int, amount: Int): Boolean

}