package pt.isel.repo.jdbi.sql

object WalletSql {
    const val SELECT_WALLET = """
        SELECT user_id, amount_coins 
        FROM wallet 
        WHERE user_id = :user_id
    """

    const val SELECT_ALL_WALLET = """
            SELECT user_id, amount_coins
            FROM wallet
        """

    const val UPDATE_WALLET = """
            UPDATE wallet
            SET amount_coins = :amount
            WHERE user_id = :userId;
    """

    const val DELETE_BY_ID = """
        DELETE FROM wallet WHERE user_id = :user_id
    """

    const val CLEAR_WALLET = """
        DELETE FROM wallet
    """

    const val CREATE_WALLET = """
        INSERT INTO wallet (user_id) VALUES (:user_id);
    """

    const val INSERT_TRANSACTION = """
        INSERT INTO wallet (user_id, round_id, amount_coins)
        VALUES (:userId, :roundId, :amount)
        RETURNING id
    """

    const val DELETE_USER_TRANSACTIONS = """
        DELETE FROM wallet WHERE user_id = :userId
    """

    const val CLEAR_ALL_TRANSACTIONS = """
        DELETE FROM wallet
    """

    const val SELECT_TRANSACTIONS_BY_USER = """
        SELECT id, user_id, round_id, amount_coins, created_at
        FROM wallet
        WHERE user_id = :userId
        ORDER BY created_at DESC
    """

    const val SELECT_TRANSACTIONS_BY_ROUND = """
        SELECT id, user_id, round_id, amount_coins, created_at
        FROM wallet
        WHERE user_id = :userId AND round_id = :roundId
        ORDER BY created_at DESC
    """

    const val SELECT_USER_BALANCE = """
        SELECT COALESCE(SUM(amount_coins), 0) as balance
        FROM wallet
        WHERE user_id = :userId
    """

    const val SELECT_LAST_TRANSACTION_DATE = """
        SELECT created_at
        FROM wallet
        WHERE user_id = :userId
        ORDER BY created_at DESC
        LIMIT 1
    """

    // Substituição da consulta por tipo
    const val SELECT_TRANSACTIONS_BY_AMOUNT_SIGN = """
        SELECT id, user_id, round_id, amount_coins, created_at
        FROM wallet
        WHERE user_id = :userId AND SIGN(amount_coins) = :sign
        ORDER BY created_at DESC
    """
}
