package br.pucpr.prissma_server.workspaces;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Contexto de tenant da request: qual workspace está ativo e qual o papel do
 * usuário NELE. Resolvido pelo {@link WorkspaceContextFilter} e carregado nos
 * details do Authentication.
 *
 * Pode ser null (request sem workspace resolvível — ex.: membro puro recém
 * convidado, staff sem conta própria). Endpoints escopados tratam null como
 * "não alcança nada" (404 genérico via ProjectPermissionService); endpoints
 * de conta (GET/POST /workspaces) funcionam sem contexto.
 */
public record WorkspaceContext(Long workspaceId, WorkspaceRole role, boolean owner) {

    public boolean isElevated() {
        return role == WorkspaceRole.OWNER || role == WorkspaceRole.ADMIN;
    }

    /** Contexto da request atual, ou null se nenhum workspace foi resolvido. */
    public static WorkspaceContext current() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getDetails() instanceof WorkspaceContext ctx)) {
            return null;
        }
        return ctx;
    }
}
