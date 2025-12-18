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
            SET amount_coins = :amount_coins
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
}
