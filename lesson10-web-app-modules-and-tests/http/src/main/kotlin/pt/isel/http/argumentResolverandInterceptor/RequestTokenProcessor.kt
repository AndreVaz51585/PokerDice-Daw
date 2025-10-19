package pt.isel.http.argumentResolverandInterceptor

import org.springframework.stereotype.Component
import pt.isel.domain.user.AuthenticatedUser
import pt.isel.service.userService.UserAuthService

@Component
class RequestTokenProcessor(
    val usersService: UserAuthService,
) {
    fun processAuthorizationHeaderValue(authorizationValue: String?): AuthenticatedUser? {
        if (authorizationValue == null) {
            return null
        }
        val parts = authorizationValue.trim().split(" ")
        if (parts.size != 2) {
            return null
        }
        if (parts[0].lowercase() != SCHEME) {
            return null
        }
        return usersService.getUserByToken(parts[1])?.let {
            AuthenticatedUser(
                it,
                parts[1],
            )
        }
    }

    companion object {
        const val SCHEME = "bearer"
    }
}
