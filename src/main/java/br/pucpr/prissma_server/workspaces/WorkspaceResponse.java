package br.pucpr.prissma_server.workspaces;

public record WorkspaceResponse(
        Long id,
        String name,
        String document,
        String status,
        boolean isPrimary,
        boolean isOwner
) {
    public static WorkspaceResponse from(Workspace workspace, Long userId) {
        return new WorkspaceResponse(
                workspace.getId(),
                workspace.getName(),
                workspace.getDocument(),
                workspace.getStatus(),
                workspace.isPrimary(),
                workspace.getOwnerId().equals(userId)
        );
    }
}
