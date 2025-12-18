package pt.isel.repo.jdbi

import org.jdbi.v3.core.Handle
import org.jdbi.v3.core.kotlin.mapTo
import pt.isel.domain.user.User
import pt.isel.domain.user.invitation.Invitation
import pt.isel.domain.user.invitation.InvitationId
import pt.isel.repo.RepositoryInvitation
import pt.isel.repo.RepositoryUser
import java.time.Instant

class RepositoryInvitationJdbi(
    private val handle: Handle,
    private val repoUser: RepositoryUser,
) : RepositoryInvitation {
    override fun findByInvitationId(invitationId: InvitationId): Invitation? {
        val invitationQuery =
            handle.createQuery(
                """
            SELECT code, created_by, created_at, used_by, used_at 
            FROM invitation WHERE code = :code
            """,
            )
                .bind("code", invitationId.value)
                .mapTo<InvitationDb>()
                .singleOrNull() ?: return null

        val createdByUser =
            repoUser.findById(invitationQuery.created_by)
                ?: throw IllegalStateException("Invitation creator with id ${invitationQuery.created_by} not found.")

        val usedByUser: User? =
            invitationQuery.used_by?.let {
                repoUser.findById(it)
                    ?: throw IllegalStateException("Invitation user with id $it not found.")
            }

        return Invitation(
            id = InvitationId(invitationQuery.code),
            createdBy = createdByUser,
            createdAt = invitationQuery.created_at,
            usedBy = usedByUser,
            usedAt = invitationQuery.used_at,
        )
    }

    override fun deleteByInvitationId(invitationId: InvitationId): Boolean {
        return handle.createUpdate("DELETE FROM invitation WHERE code = :code")
            .bind("code", invitationId.value)
            .execute() > 0
    }

    override fun create(invitation: Invitation): Invitation {
        handle.createUpdate(
            """
            INSERT INTO invitation(code, created_by, created_at, used_by, used_at) 
            VALUES (:code, :created_by, :created_at, :used_by, :used_at)
            """,
        )
            .bind("code", invitation.id.value)
            .bind("created_by", invitation.createdBy.id)
            .bind("created_at", invitation.createdAt)
            .bind("used_by", invitation.usedBy?.id)
            .bind("used_at", invitation.usedAt)
            .execute()
        return invitation
    }

    override fun findById(id: Int): Invitation? =
        throw UnsupportedOperationException(
            "Find by integer ID is not supported for Invitation.",
        )

    override fun findAll(): List<Invitation> = throw UnsupportedOperationException("Find all is not supported for Invitation.")

    override fun save(entity: Invitation) {
        handle.createUpdate(
            """
            UPDATE invitation 
            SET used_by = :used_by, used_at = :used_at 
            WHERE code = :code
            """,
        )
            .bind("code", entity.id.value)
            .bind("used_by", entity.usedBy?.id)
            .bind("used_at", entity.usedAt)
            .execute()
    }

    override fun deleteById(id: Int): Boolean =
        throw UnsupportedOperationException(
            "Delete by integer ID is not supported for Invitation.",
        )

    override fun clear() {
        handle.createUpdate("DELETE FROM invitation").execute()
    }

    // ---------------------------
    // Helpers
    // ---------------------------
    private data class InvitationDb(
        val code: String,
        val created_by: Int,
        val created_at: Instant,
        val used_by: Int?,
        val used_at: Instant?,
    )
}
