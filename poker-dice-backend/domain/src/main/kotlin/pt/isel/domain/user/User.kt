package pt.isel.domain.user

import pt.isel.domain.authentication.PasswordValidationInfo

data class User(
    val id: Int,
    val name: String,
    val email: String,
    val passwordValidation: PasswordValidationInfo,
)
