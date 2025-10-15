package pt.isel.repo.jdbi.sql

object UserSql {
    const val FIND_BY_ID = """
        SELECT * FROM dbo.users WHERE id = :id
    """

    const val FIND_ALL = """
        SELECT * FROM dbo.users
    """

    const val UPDATE_USER = """
        UPDATE dbo.users
        SET name = :name, email = :email
        WHERE id = :id
    """

    const val DELETE_BY_ID = """
        DELETE FROM dbo.users WHERE id = :id
    """

    const val CLEAR_TOKENS = """
        DELETE FROM dbo.Tokens
    """

    const val CLEAR_USERS = """
        DELETE FROM dbo.users
    """

    const val CREATE_USER = """
        INSERT INTO dbo.users (name, email, password_validation)
        VALUES (:name, :email, :password_validation)
        RETURNING id
    """

    const val FIND_BY_EMAIL = """
        SELECT * FROM dbo.users WHERE email = :email
    """

    const val GET_TOKEN_BY_VALIDATION = """
        SELECT id, name, email, password_validation, token_validation, created_at, last_used_at
        FROM dbo.Users as users
        INNER JOIN dbo.Tokens as tokens
        ON users.id = tokens.user_id
        WHERE token_validation = :validation_information
    """

    const val DELETE_OLDEST_TOKENS = """
        DELETE FROM dbo.Tokens
        WHERE user_id = :user_id
            AND token_validation IN (
                SELECT token_validation FROM dbo.Tokens WHERE user_id = :user_id
                ORDER BY last_used_at DESC OFFSET :offset
            )
    """

    const val CREATE_TOKEN = """
        INSERT INTO dbo.Tokens(user_id, token_validation, created_at, last_used_at)
        VALUES (:user_id, :token_validation, :created_at, :last_used_at)
    """

    const val UPDATE_TOKEN_LAST_USED = """
        UPDATE dbo.Tokens
        SET last_used_at = :last_used_at
        WHERE token_validation = :validation_information
    """

    const val REMOVE_TOKEN_BY_VALIDATION = """
        DELETE FROM dbo.Tokens
        WHERE token_validation = :validation_information
    """
}
