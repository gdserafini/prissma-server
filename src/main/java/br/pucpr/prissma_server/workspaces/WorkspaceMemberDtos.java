package br.pucpr.prissma_server.workspaces;

import java.time.Instant;

public final class WorkspaceMemberDtos {

    private WorkspaceMemberDtos() {
    }

    public record InviteMemberRequest(String email, String fullName, String role) {
    }

    /**
     * Resposta do convite. É EXATAMENTE o mesmo payload quer o convite tenha
     * sido criado, renovado ou silenciosamente descartado (e-mail de staff) —
     * a resposta nunca pode funcionar como sonda de existência de conta.
     */
    public record MemberInviteResponse(String invitedEmail, String role, Instant expiresAt) {
    }

    /** Aceite público: cria a conta se o e-mail ainda não existir. */
    public record AcceptInviteRequest(String fullName, String password) {
    }

    public record UpdateWorkspaceMemberRequest(String role) {
    }

    public record WorkspaceMemberResponse(
            Long id,
            Long userId,
            String name,
            String email,
            String role,
            boolean active,
            Instant acceptedAt
    ) {
    }
}
