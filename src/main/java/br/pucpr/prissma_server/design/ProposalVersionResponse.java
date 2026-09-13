package br.pucpr.prissma_server.design;

import java.time.Instant;

public record ProposalVersionResponse(
        Long id,
        int version,
        ProposalStatus status,
        String description,
        Long authorUserId,
        String authorName,
        boolean generatedByAi,
        /** A tela usa isto para decidir entre buscar a imagem e desenhar a hachura. */
        boolean hasImage,
        String fileName,
        String fileType,
        Instant submittedAt,
        Instant updatedAt
) {
    public static ProposalVersionResponse from(DesignSubmission submission) {
        return new ProposalVersionResponse(
                submission.getId(),
                submission.getVersion(),
                submission.getStatus(),
                submission.getDescription(),
                submission.getAuthorUser() != null ? submission.getAuthorUser().getId() : null,
                submission.getAuthorName(),
                submission.isGeneratedByAi(),
                submission.getFileUrl() != null && !submission.getFileUrl().isBlank(),
                submission.getFileName(),
                submission.getFileType(),
                submission.getSubmittedAt(),
                submission.getUpdatedAt()
        );
    }
}
