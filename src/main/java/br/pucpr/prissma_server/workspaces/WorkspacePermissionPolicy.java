package br.pucpr.prissma_server.workspaces;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Política de permissão do NÍVEL workspace — mapa FIXO em código, sem tabela
 * de customização (YAGNI; a customização por obra continua em
 * project_role_permissions).
 *
 *   OWNER:  tudo
 *   ADMIN:  convida/gerencia membros, cria obras, vê todas
 *   MEMBER: nada no nível da conta — age via papéis de obra
 *   CLIENT: idem (perfil só-leitura do domínio; nem vê a equipe)
 */
public final class WorkspacePermissionPolicy {

    private static final Map<WorkspaceRole, Set<WorkspaceAction>> POLICY = new EnumMap<>(Map.of(
            WorkspaceRole.OWNER, EnumSet.allOf(WorkspaceAction.class),
            WorkspaceRole.ADMIN, EnumSet.of(
                    WorkspaceAction.INVITE_MEMBERS,
                    WorkspaceAction.MANAGE_MEMBERS,
                    WorkspaceAction.CREATE_PROJECT,
                    WorkspaceAction.VIEW_ALL_PROJECTS),
            WorkspaceRole.MEMBER, EnumSet.noneOf(WorkspaceAction.class),
            WorkspaceRole.CLIENT, EnumSet.noneOf(WorkspaceAction.class)
    ));

    private WorkspacePermissionPolicy() {
    }

    public static boolean allows(WorkspaceRole role, WorkspaceAction action) {
        return role != null && POLICY.get(role).contains(action);
    }

    /** 403 quando o papel da conta não cobre a ação. A checagem de permissão vem SEMPRE antes de validar entrada. */
    public static void require(WorkspaceContext ctx, WorkspaceAction action) {
        if (ctx == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Workspace not found");
        }
        if (!allows(ctx.role(), action)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Workspace role does not allow this operation: " + action);
        }
    }
}
