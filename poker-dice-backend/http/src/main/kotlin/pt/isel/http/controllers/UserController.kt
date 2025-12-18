package pt.isel.http.controllers

import jakarta.servlet.http.Cookie
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import pt.isel.domain.token.TokenExternalInfo
import pt.isel.domain.user.AuthenticatedUser
import pt.isel.domain.user.User
import pt.isel.http.argumentResolverandInterceptor.RequestTokenProcessor
import pt.isel.http.model.problem.Problem
import pt.isel.http.model.user.UserCreateTokenInputModel
import pt.isel.http.model.user.UserCreateTokenOutputModel
import pt.isel.http.model.user.UserHomeOutputModel
import pt.isel.http.model.user.UserInput
import pt.isel.service.Auxiliary.Either
import pt.isel.service.Auxiliary.Failure
import pt.isel.service.Auxiliary.Success
import pt.isel.service.userService.UserAuthService
import pt.isel.service.userService.UserError
import pt.isel.service.walletService.WalletService

@RestController
class UserController(
    private val userService: UserAuthService,
    private val walletService: WalletService,
) {
    /**
     * Try with:
     curl -i -X POST http://localhost:8080/api/users \
     -H "Content-Type: application/json" \
     -d '{
     "name": "Paul Atreides",
     "email": "paul@atreides.com",
     "password": "muadib"
     }'
     */
    @PostMapping("/api/users")
    fun createUser(
        @RequestBody userInput: UserInput,
    ): ResponseEntity<*> {
        val result: Either<UserError, User> =
            userService
                .createUser(userInput.name, userInput.email, userInput.password, userInput.invitationCode)

        return when (result) {
            is Success ->
                ResponseEntity
                    .status(HttpStatus.CREATED)
                    .header(
                        "Location",
                        "/api/users/${result.value.id}",
                    ).build<Unit>()

            is Failure ->
                when (result.value) {
                    is UserError.AlreadyUsedEmailAddress ->
                        Problem.EmailAlreadyInUse.response(
                            HttpStatus.CONFLICT,
                        )

                    UserError.InsecurePassword ->
                        Problem.InsecurePassword.response(
                            HttpStatus.BAD_REQUEST,
                        )

                    UserError.InvitationCodeRequired ->
                        Problem.InvitationCodeRequired.response(
                            HttpStatus.BAD_REQUEST,
                        )

                    UserError.InvalidInvitationCode ->
                        Problem.InvalidInvitationCode.response(
                            HttpStatus.BAD_REQUEST,
                        )

                    UserError.InvitationCodeAlreadyUsed ->
                        Problem.InvitationCodeAlreadyUsed.response(
                            HttpStatus.CONFLICT,
                        )

                    else -> Problem.ErrorCreatingUser.response(HttpStatus.INTERNAL_SERVER_ERROR)
                }
        }
    }

    /**
     * Try with:
     curl -i -X POST http://localhost:8080/api/users/token \
     -H "Content-Type: application/json" \
     -d '{
     "email": "paul@atreides.com",
     "password": "muadib"
     }'
     */
    @PostMapping("/api/users/token")
    fun token(
        @RequestBody input: UserCreateTokenInputModel,
        response: HttpServletResponse
    ): ResponseEntity<*> {
        val tokenInfo: Either<UserError, TokenExternalInfo> = userService.createToken(input.email, input.password)

        return when (tokenInfo) {
            is Success -> {

                val cookie = Cookie(RequestTokenProcessor.COOKIE_NAME, tokenInfo.value.tokenValue).apply {
                    isHttpOnly = true
                    secure = false // true apenas em produção com HTTPS
                    path = "/"
                    maxAge = RequestTokenProcessor.COOKIE_MAX_AGE
                    setAttribute("SameSite", "Strict")


                }

                response.addCookie(cookie)


                ResponseEntity
                    .status(HttpStatus.OK)
                    .body(UserCreateTokenOutputModel(tokenInfo.value.tokenValue))
            }

            is Failure -> {
                when (tokenInfo.value) {
                    UserError.UserNotFound -> Problem.UserNotFound.response(HttpStatus.NOT_FOUND)

                    UserError.InvalidCredentials ->
                        Problem.InvalidCredentials.response(
                            HttpStatus.UNAUTHORIZED,
                        )

                    else -> {
                        Problem.Unknown.response(HttpStatus.INTERNAL_SERVER_ERROR)
                    }
                }
            }
        }
    }

    /**
     * This handler requires an authenticated user.
     * The {@link AuthenticatedUser} is resolved by an ArgumentResolver
     * using data extracted from the HTTP request headers.
     * Try:

     curl -i -X POST http://localhost:8080/api/logout
     -H "Authorization: Bearer lCZVAG-_OZx0Fq52MllDklc706vnLjGPWaMwRXKHJTM="

     */
    @PostMapping("api/logout")
    fun logout(
        user: AuthenticatedUser,
        response: HttpServletResponse
        ) {
        userService.revokeToken(user.token)

        val cookie = Cookie(RequestTokenProcessor.COOKIE_NAME, "").apply {
            maxAge = 0
            path = "/"
        }

        response.addCookie(cookie)

    }

    @DeleteMapping("/api/users/{id}")
    fun deleteUser(
        @PathVariable id: Int,
    ): ResponseEntity<*> {
        val result: Either<UserError, Boolean> = userService.deleteUser(id)
        return when (result) {
            is Success -> ResponseEntity.status(HttpStatus.OK).build<Unit>()
            is Failure ->
                when (result.value) {
                    UserError.UserNotFound -> Problem.UserNotFound.response(HttpStatus.NOT_FOUND)
                    UserError.ErrorDeletingUser -> Problem.ErrorDeletingUser.response(HttpStatus.INTERNAL_SERVER_ERROR)
                    else -> Problem.Unknown.response(HttpStatus.INTERNAL_SERVER_ERROR)
                }
        }
    }

    @GetMapping("/api/users")
    fun getAllUsers(): ResponseEntity<*> {
        val users: List<User> = userService.getAllUsers()
        return ResponseEntity
            .status(HttpStatus.OK)
            .body(users)
    }

    /**
     * This handler requires an authenticated user.
     * The {@link AuthenticatedUser} is resolved by an ArgumentResolver
     * using data extracted from the HTTP request headers.
     * Try:

     curl -i http://localhost:8080/api/me \
     -H "Authorization: Bearer lCZVAG-_OZx0Fq52MllDklc706vnLjGPWaMwRXKHJTM="

     */
    @GetMapping("/api/me")
    fun userHome(userAuthenticatedUser: AuthenticatedUser): ResponseEntity<UserHomeOutputModel> =
        ResponseEntity
            .status(HttpStatus.OK)
            .body(
                UserHomeOutputModel(
                    id = userAuthenticatedUser.user.id,
                    name = userAuthenticatedUser.user.name,
                    email = userAuthenticatedUser.user.email,
                ),
            )
}
