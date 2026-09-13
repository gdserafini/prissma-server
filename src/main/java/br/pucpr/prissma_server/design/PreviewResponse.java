package br.pucpr.prissma_server.design;

import java.time.Instant;

/**
 * O que a tela consulta em polling enquanto a IA trabalha.
 *
 * {@code versionId} só vem preenchido quando o status é {@code READY}; a tela
 * então recarrega a proposta e a versão nova aparece no card.
 */
public record PreviewResponse(
        Long id,
        Long proposalId,
        PreviewStatus status,
        Long versionId,
        String errorMessage,
        Instant createdAt,
        Instant completedAt
) {
    public static PreviewResponse from(EnvironmentPreview preview) {
        return new PreviewResponse(
                preview.getId(),
                preview.getProposal() != null ? preview.getProposal().getId() : null,
                preview.getStatus(),
                preview.getResultSubmission() != null ? preview.getResultSubmission().getId() : null,
                preview.getErrorMessage(),
                preview.getCreatedAt(),
                preview.getCompletedAt()
        );
    }
}
