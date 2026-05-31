package br.pucpr.prissma_server.projects;

import java.util.List;

public record UpdateRolePermissionsRequest(List<String> permissions) {
}
