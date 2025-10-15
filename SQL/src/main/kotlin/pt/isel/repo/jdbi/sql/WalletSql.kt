package pt.isel.repo.jdbi.sql

object WalletSql {
    const val SELECT_DISTINCT_USERS = """
        SELECT DISTINCT user_id FROM wallet_tx
    """

    const val INSERT_TRANSACTION = """
        INSERT INTO wallet_tx (user_id, round_id, amount_coins)
        VALUES (:userId, :roundId, :amount)
        RETURNING id
    """

    const val DELETE_USER_TRANSACTIONS = """
        DELETE FROM wallet_tx WHERE user_id = :userId
    """

    const val CLEAR_ALL_TRANSACTIONS = """
        DELETE FROM wallet_tx
    """

    const val SELECT_TRANSACTIONS_BY_USER = """
        SELECT id, user_id, round_id, amount_coins, created_at
        FROM wallet_tx
        WHERE user_id = :userId
        ORDER BY created_at DESC
    """

    const val SELECT_TRANSACTIONS_BY_ROUND = """
        SELECT id, user_id, round_id, amount_coins, created_at
        FROM wallet_tx
        WHERE user_id = :userId AND round_id = :roundId
        ORDER BY created_at DESC
    """

    const val SELECT_USER_BALANCE = """
        SELECT COALESCE(SUM(amount_coins), 0) as balance
        FROM wallet_tx
        WHERE user_id = :userId
    """

    const val SELECT_LAST_TRANSACTION_DATE = """
        SELECT created_at
        FROM wallet_tx
        WHERE user_id = :userId
        ORDER BY created_at DESC
        LIMIT 1
    """

    // Substituição da consulta por tipo
    const val SELECT_TRANSACTIONS_BY_AMOUNT_SIGN = """
        SELECT id, user_id, round_id, amount_coins, created_at
        FROM wallet_tx
        WHERE user_id = :userId AND SIGN(amount_coins) = :sign
        ORDER BY created_at DESC
    """
}
