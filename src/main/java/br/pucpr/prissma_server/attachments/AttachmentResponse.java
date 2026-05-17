package br.pucpr.prissma_server.attachments;

import java.time.Instant;

public record AttachmentResponse(
        Long id,
        Long constructionProjectId,
        Long stageId,
        Long taskId,
        Long uploadedByUserId,
        String fileName,
        String fileType,
        Instant uploadedAt
) {
    public static AttachmentResponse from(Attachment a) {
        return new AttachmentResponse(
                a.getId(),
                a.getConstructionProject() != null ? a.getConstructionProject().getId() : null,
                a.getStage() != null ? a.getStage().getId() : null,
                a.getTask() != null ? a.getTask().getId() : null,
                a.getUploadedByUser() != null ? a.getUploadedByUser().getId() : null,
                a.getFileName(),
                a.getFileType(),
                a.getUploadedAt()
        );
    }
}
