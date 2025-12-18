package pt.isel.http.controllers

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RestController
import pt.isel.domain.user.AuthenticatedUser
import pt.isel.domain.user.invitation.InvitationId
import pt.isel.service.userService.InvitationService

@RestController
class InvitationController(private val invitationService: InvitationService) {
    @PostMapping("/api/invitations")
    fun createInvitation(user: AuthenticatedUser): ResponseEntity<InvitationId> {
        val invitationId = invitationService.createInvitation(user.user)
        return ResponseEntity.ok(invitationId)
    }
}
