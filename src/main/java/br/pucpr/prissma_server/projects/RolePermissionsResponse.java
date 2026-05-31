package br.pucpr.prissma_server.projects;

import java.util.List;

public record RolePermissionsResponse(String role, List<String> permissions) {
}
