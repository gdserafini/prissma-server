package br.pucpr.prissma_server.diary;

import java.time.Instant;

public record DiaryEntryResponse(
        Long id,
        Long constructionProjectId,
        Instant entryDate,
        DiaryEntryType entryType,
        Long responsibleUserId,
        String responsibleName,
        String description,
        Long attachmentId,
        String attachmentFileName,
        Instant createdAt,
        Instant updatedAt
) {
    public static DiaryEntryResponse from(DiaryEntry entry) {
        return new DiaryEntryResponse(
                entry.getId(),
                entry.getConstructionProject() != null ? entry.getConstructionProject().getId() : null,
                entry.getEntryDate(),
                entry.getEntryType(),
                entry.getResponsibleUser() != null ? entry.getResponsibleUser().getId() : null,
                entry.getResponsibleName(),
                entry.getDescription(),
                entry.getAttachment() != null ? entry.getAttachment().getId() : null,
                entry.getAttachment() != null ? entry.getAttachment().getFileName() : null,
                entry.getCreatedAt(),
                entry.getUpdatedAt()
        );
    }
}
