package pt.isel.http.model.user

data class UserInput(
    val name: String,
    val email: String,
    val password: String,
    val invitationCode: String?,
)
