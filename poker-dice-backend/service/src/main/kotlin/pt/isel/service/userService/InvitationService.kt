package pt.isel.service.userService

import org.springframework.stereotype.Service
import pt.isel.domain.user.User
import pt.isel.domain.user.invitation.Invitation
import pt.isel.domain.user.invitation.InvitationId
import pt.isel.repo.RepositoryInvitation
import java.security.SecureRandom
import java.time.Instant
import java.util.Base64

@Service
class InvitationService(
    private val repoInvitation: RepositoryInvitation,
) {
    fun createInvitation(user: User): InvitationId {
        val code = generateCode()

        val invitation =
            Invitation(
                id = InvitationId(code),
                createdBy = user,
                createdAt = Instant.now(),
            )

        repoInvitation.create(invitation)
        return invitation.id
    }

    private fun generateCode(): String {
        val bytes = ByteArray(8)
        SecureRandom().nextBytes(bytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }
}
