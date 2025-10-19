package pt.isel.http.model.user

data class UserCreateTokenInputModel(
    val email: String,
    val password: String,
)
