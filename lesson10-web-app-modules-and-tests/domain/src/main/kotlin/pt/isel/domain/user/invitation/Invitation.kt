package pt.isel.domain.user.invitation

import pt.isel.domain.user.User
import java.time.Instant

data class Invitation(
    val id: InvitationId,
    val createdBy: User,
    val createdAt: Instant,
    val usedBy: User? = null,
    val usedAt: Instant? = null,
)
