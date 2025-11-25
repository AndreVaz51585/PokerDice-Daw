package pt.isel.repo.jdbi

import org.jdbi.v3.core.Handle
import org.springframework.stereotype.Repository
import pt.isel.domain.Game.money.Wallet
import pt.isel.repo.RepositoryWallet
import pt.isel.repo.jdbi.sql.WalletSql

class RepositoryWalletJdbi(private val handle: Handle) : RepositoryWallet {
    
    override fun createWallet(user_id: Int): Wallet{
        handle.createUpdate(
                WalletSql.CREATE_WALLET
            )
            .bind("user_id", user_id)
            .execute()
        return Wallet(user_id, 0)
    }


    override fun findById(id: Int): Wallet? {
        val wallet = handle.createQuery(WalletSql.SELECT_WALLET)
            .bind("user_id", id)
            .map { rs, _ ->
                Wallet(
                    userId = rs.getInt("user_id"),
                    currentBalance = rs.getInt("amount_coins"),
                )
            }
            .findOne()
            .orElse(null)
            ?: return null

        return wallet
    }

    override fun findAll(): List<Wallet> {
        val wallet = handle.createQuery(WalletSql.SELECT_ALL_WALLET)
            .map { rs, _ ->
                Wallet(
                    userId = rs.getInt("user_id"),
                    currentBalance = rs.getInt("amount_coins")
                )
            }
            .list()

        // N+1. Para volume elevado, considerar join + agregação.
        return wallet
    }

    override fun save(entity: Wallet) {
        handle.createUpdate(WalletSql.UPDATE_WALLET)
            .bind("amount_coins", entity.currentBalance)
            .bind("userId", entity.userId)
            .execute()
    }


    override fun deleteById(id: Int): Boolean {
        val ret = handle.createUpdate(WalletSql.DELETE_BY_ID)
            .bind("user_id", id)
            .execute() > 0
        return ret
    }

    override fun clear() {
        handle.createUpdate(WalletSql.CLEAR_WALLET).execute()
    }

    override fun getBalance(userId: Int): Int? {
        val wallet = handle.createQuery(WalletSql.SELECT_WALLET)
            .bind("user_id", userId)
            .map { rs, _ -> rs.getInt("amount_coins")
            }
            .findOne()
            .orElse(null)
        return wallet
    }

}