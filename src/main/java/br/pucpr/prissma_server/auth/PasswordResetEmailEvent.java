package br.pucpr.prissma_server.auth;

public record PasswordResetEmailEvent(String to, String resetLink) {
}
