package br.pucpr.prissma_server.projects;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.util.EnumSet;
import java.util.Set;
import static br.pucpr.prissma_server.projects.ProjectPermission.*;

public enum ProjectRole {

    OWNER(EnumSet.allOf(ProjectPermission.class)),

    ENGINEER(EnumSet.of(VIEW_PROJECT, MANAGE_PROJECT, MANAGE_MEMBERS, MANAGE_BUDGET,
            MANAGE_STAGES, MANAGE_TEAMS, MANAGE_TASKS, MANAGE_ATTACHMENTS, MANAGE_DIARY)),

    // A matriz do design dá "w" em Tarefas ao arquiteto (decisão A1) e esconde
    // Orçamento do mestre (decisão A2) — os defaults abaixo espelham o design.
    // Obras com override em project_role_permissions não são afetadas.
    ARCHITECT(EnumSet.of(VIEW_PROJECT, MANAGE_TASKS, MANAGE_ATTACHMENTS)),

    FOREMAN(EnumSet.of(VIEW_PROJECT, MANAGE_BUDGET, MANAGE_TEAMS,
            MANAGE_TASKS, MANAGE_ATTACHMENTS, MANAGE_DIARY)),

    USER(EnumSet.of(VIEW_PROJECT));

    private final Set<ProjectPermission> defaultPermissions;

    ProjectRole(Set<ProjectPermission> defaultPermissions) {
        this.defaultPermissions = defaultPermissions;
    }

    public Set<ProjectPermission> getDefaultPermissions() {
        return EnumSet.copyOf(defaultPermissions);
    }

    public static ProjectRole fromString(String value) {
        if (value == null || value.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Role in project is required");
        }
        try {
            return ProjectRole.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Role in project must be one of OWNER, ENGINEER, ARCHITECT, FOREMAN or USER");
        }
    }
}
