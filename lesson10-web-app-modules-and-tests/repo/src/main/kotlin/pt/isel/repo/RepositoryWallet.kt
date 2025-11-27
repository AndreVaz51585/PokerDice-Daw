package pt.isel.repo

import pt.isel.domain.Game.money.Wallet

interface RepositoryWallet : Repository<Wallet> {

    fun createWallet(user_id: Int): Wallet

    // Obtém o saldo atual (soma de todas as transações) para um usuário
    fun getBalance(userId: Int): Int?

}