package pt.isel.repo

import pt.isel.domain.user.invitation.Invitation
import pt.isel.domain.user.invitation.InvitationId

interface RepositoryInvitation : Repository<Invitation> {
    fun findByInvitationId(invitationId: InvitationId): Invitation?

    fun deleteByInvitationId(invitationId: InvitationId): Boolean

    fun create(invitation: Invitation): Invitation
}
