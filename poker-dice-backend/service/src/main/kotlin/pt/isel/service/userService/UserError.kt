package pt.isel.service.userService

sealed class UserError {
    data object AlreadyUsedEmailAddress : UserError()

    data object InsecurePassword : UserError()

    data object UserNotFound : UserError()

    data object ErrorDeletingUser : UserError()

    data object InvitationCodeRequired : UserError()

    data object InvalidInvitationCode : UserError()

    data object InvitationCodeAlreadyUsed : UserError()

    data object InvalidCredentials : UserError()
}

sealed class TokenCreationError {
    data object UserOrPasswordAreInvalid : TokenCreationError()
}
