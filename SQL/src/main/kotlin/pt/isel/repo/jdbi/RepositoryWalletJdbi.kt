package pt.isel.repo.jdbi

import org.jdbi.v3.core.Handle
import org.jdbi.v3.core.Jdbi
import pt.isel.domain.Game.money.Wallet
import pt.isel.domain.Game.money.WalletTransaction
import pt.isel.repo.RepositoryWallet
import pt.isel.repo.jdbi.sql.WalletSql
import java.time.Instant

class RepositoryWalletJdbi(private val jdbi: Jdbi) : RepositoryWallet {

    override fun findById(id: Int): Wallet? {
        return jdbi.withHandle<Wallet?, Exception> { handle ->
            val userId = id.toLong()
            val balance = getBalanceForUser(handle, id)
            if (balance != null) {
                Wallet(
                    userId = userId.toInt(),
                    currentBalance = balance
                )
            } else {
                null
            }
        }
    }

    override fun findAll(): List<Wallet> {
        return jdbi.withHandle<List<Wallet>, Exception> { handle ->
            val userIds = handle.createQuery(WalletSql.SELECT_DISTINCT_USERS).mapTo(Int::class.java).list()

            userIds.map { userId ->
                val balance = getBalanceForUser(handle, userId) ?: 0
                Wallet(
                    userId = userId.toInt(),
                    currentBalance = balance,
                )
            }
        }
    }

    override fun save(entity: Wallet) {
        jdbi.useHandle<Exception> { handle ->
            val currentBalance = getBalanceForUser(handle, entity.userId.toInt()) ?: 0
            val difference = entity.currentBalance - currentBalance

            if (difference != 0) {
                handle.createUpdate(WalletSql.INSERT_TRANSACTION).bind("userId", entity.userId)
                    .bind("roundId", null as Long?)  // Especificar o tipo explicitamente
                    .bind("amount", difference).execute()
            }
        }
    }

    override fun deleteById(id: Int): Boolean {
        return jdbi.withHandle<Boolean, Exception> { handle ->
            val rowsAffected = handle.createUpdate(WalletSql.DELETE_USER_TRANSACTIONS).bind("userId", id).execute()
            rowsAffected > 0
        }
    }

    override fun clear() {
        jdbi.useHandle<Exception> { handle ->
            handle.execute(WalletSql.CLEAR_ALL_TRANSACTIONS)
        }
    }


    override fun addTransaction(
        userId: Int, amount: Int, roundId: Int?
    ): Int {
        if (amount == 0) throw IllegalArgumentException("Transação com valor zero não é permitida")

        return jdbi.withHandle<Int, Exception> { handle ->
            handle.createUpdate(WalletSql.INSERT_TRANSACTION).bind("userId", userId).bind("roundId", roundId)
                .bind("amount", amount).executeAndReturnGeneratedKeys("id").mapTo(Int::class.java).one()
        }
    }

    override fun getBalance(userId: Int): Int {
        return jdbi.withHandle<Int, Exception> { handle ->
            getBalanceForUser(handle, userId) ?: 0
        }
    }

    override fun getTransactionHistory(userId: Int): List<WalletTransaction> {
        return jdbi.withHandle<List<WalletTransaction>, Exception> { handle ->
            handle.createQuery(WalletSql.SELECT_TRANSACTIONS_BY_USER).bind("userId", userId).map { rs, _ ->
                    WalletTransaction(
                        id = rs.getInt("id"),
                        userId = rs.getInt("user_id"),
                        roundId = rs.getObject("round_id") as? Int,
                        amount = rs.getInt("amount_coins"),
                        createdAt = rs.getTimestamp("created_at").toInstant()
                    )
                }.list()
        }
    }

    override fun getTransactionsByRound(userId: Int, roundId: Int): List<WalletTransaction> {
        return jdbi.withHandle<List<WalletTransaction>, Exception> { handle ->
            handle.createQuery(WalletSql.SELECT_TRANSACTIONS_BY_ROUND).bind("userId", userId).bind("roundId", roundId)
                .map { rs, _ ->
                    WalletTransaction(
                        id = rs.getInt("id"),
                        userId = rs.getInt("user_id"),
                        roundId = rs.getObject("round_id") as? Int,
                        amount = rs.getInt("amount_coins"),
                        createdAt = rs.getTimestamp("created_at").toInstant()
                    )
                }.list()
        }
    }

    override fun hasSufficientFunds(userId: Int, amount: Int): Boolean {
        if (amount <= 0) return true

        val balance = getBalance(userId)
        return balance >= amount
    }

    // Métodos auxiliares privados
    private fun getBalanceForUser(handle: Handle, userId: Int): Int? {
        return handle.createQuery(WalletSql.SELECT_USER_BALANCE).bind("userId", userId).mapTo(Int::class.java).one()
    }

    private fun getLastTransactionDate(handle: Handle, userId: Int): Instant? {
        return handle.createQuery(WalletSql.SELECT_LAST_TRANSACTION_DATE).bind("userId", userId)
            .mapTo(Instant::class.java).findOne().orElse(null)
    }
}
