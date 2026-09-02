package br.pucpr.prissma_server.workspaces;

public record InviteEmailEvent(String to, String workspaceName, String inviteLink) {
}
