package br.pucpr.prissma_server.auth;

public record ResetPasswordRequest(String token, String newPassword) {
}
