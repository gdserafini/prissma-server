package br.pucpr.prissma_server.workspaces;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * Papel do usuário NA CONTA (workspace). Não confundir com ProjectRole,
 * o papel NA OBRA:
 *
 *   OWNER/ADMIN  -> alcançam todas as obras do workspace (permissões plenas)
 *   MEMBER/CLIENT -> alcançam apenas as obras onde têm linha em
 *                    construction_project_members; lá dentro vale o papel
 *                    da obra + project_role_permissions.
 */
public enum WorkspaceRole {
    OWNER,
    ADMIN,
    MEMBER,
    CLIENT;

    public static WorkspaceRole fromString(String value) {
        if (value == null || value.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Workspace role is required");
        }
        try {
            return WorkspaceRole.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Workspace role must be one of OWNER, ADMIN, MEMBER or CLIENT");
        }
    }
}
