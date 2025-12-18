package pt.isel.repo.jdbi

import org.jdbi.v3.core.Handle
import org.jdbi.v3.core.kotlin.mapTo
import org.slf4j.LoggerFactory
import pt.isel.domain.authentication.PasswordValidationInfo
import pt.isel.domain.token.Token
import pt.isel.domain.token.TokenValidationInfo
import pt.isel.domain.user.User
import pt.isel.repo.RepositoryUser
import pt.isel.repo.jdbi.sql.UserSql
import java.time.Instant

class RepositoryUserJdbi(
    private val handle: Handle,
) : RepositoryUser {
    override fun findById(id: Int): User? =
        handle
            .createQuery(UserSql.FIND_BY_ID)
            .bind("id", id)
            .mapTo<User>()
            .findOne()
            .orElse(null)

    override fun findAll(): List<User> =
        handle
            .createQuery(UserSql.FIND_ALL)
            .mapTo<User>()
            .list()

    override fun save(entity: User) {
        handle
            .createUpdate(UserSql.UPDATE_USER)
            .bindBean(entity)
            .execute()
    }

    override fun deleteById(id: Int): Boolean {
        return handle
            .createUpdate(UserSql.DELETE_BY_ID)
            .bind("id", id)
            .execute() > 0
    }

    override fun clear() {
        handle.createUpdate(UserSql.CLEAR_TOKENS).execute()
        handle.createUpdate(UserSql.CLEAR_USERS).execute()
    }

    override fun createUser(
        name: String,
        email: String,
        passwordValidation: PasswordValidationInfo,
        invitationCode: String?,
    ): User {
        val id =
            handle
                .createUpdate(UserSql.CREATE_USER)
                .bind("name", name)
                .bind("email", email)
                .bind("password_validation", passwordValidation.validationInfo)
                .executeAndReturnGeneratedKeys()
                .mapTo(Int::class.java)
                .one()

        return User(id, name, email, passwordValidation)
    }

    override fun count(): Long {
        return handle.createQuery("SELECT COUNT(*) FROM dbo.users")
            .mapTo<Long>()
            .one()
    }

    override fun findByEmail(email: String): User? =
        handle
            .createQuery(UserSql.FIND_BY_EMAIL)
            .bind("email", email)
            .mapTo<User>()
            .findOne()
            .orElse(null)

    override fun getTokenByTokenValidationInfo(tokenValidationInfo: TokenValidationInfo): Pair<User, Token>? =
        handle
            .createQuery(UserSql.GET_TOKEN_BY_VALIDATION)
            .bind("validation_information", tokenValidationInfo.validationInfo)
            .mapTo<UserAndTokenModel>()
            .singleOrNull()
            ?.userAndToken

    override fun createToken(
        token: Token,
        maxTokens: Int,
    ) {
        // Delete the oldest token when achieved the maximum number of tokens
        val deletions =
            handle
                .createUpdate(UserSql.DELETE_OLDEST_TOKENS)
                .bind("user_id", token.userId)
                .bind("offset", maxTokens - 1)
                .execute()

        logger.info("{} tokens deleted when creating new token", deletions)

        handle
            .createUpdate(UserSql.CREATE_TOKEN)
            .bind("user_id", token.userId)
            .bind("token_validation", token.tokenValidationInfo.validationInfo)
            .bind("created_at", token.createdAt.epochSecond)
            .bind("last_used_at", token.lastUsedAt.epochSecond)
            .execute()
    }

    override fun updateTokenLastUsed(
        token: Token,
        now: Instant,
    ) {
        handle
            .createUpdate(UserSql.UPDATE_TOKEN_LAST_USED)
            .bind("last_used_at", now.epochSecond)
            .bind("validation_information", token.tokenValidationInfo.validationInfo)
            .execute()
    }

    override fun removeTokenByValidationInfo(tokenValidationInfo: TokenValidationInfo): Int =
        handle
            .createUpdate(UserSql.REMOVE_TOKEN_BY_VALIDATION)
            .bind("validation_information", tokenValidationInfo.validationInfo)
            .execute()

    private data class UserAndTokenModel(
        val id: Int,
        val name: String,
        val email: String,
        val passwordValidation: PasswordValidationInfo,
        val tokenValidation: TokenValidationInfo,
        val createdAt: Long,
        val lastUsedAt: Long,
    ) {
        val userAndToken: Pair<User, Token>
            get() =
                Pair(
                    User(id, name, email, passwordValidation),
                    Token(
                        tokenValidation,
                        id,
                        Instant.ofEpochSecond(createdAt),
                        Instant.ofEpochSecond(lastUsedAt),
                    ),
                )
    }

    companion object {
        private val logger = LoggerFactory.getLogger(RepositoryUserJdbi::class.java)
    }
}
