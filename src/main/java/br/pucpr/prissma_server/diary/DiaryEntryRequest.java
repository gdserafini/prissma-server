package br.pucpr.prissma_server.diary;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public class DiaryEntryRequest {

    /** Data com dia e horario do fato registrado. */
    @NotNull(message = "Entry date is required")
    private Instant entryDate;

    /** OCCURRENCE, DELIVERY, WORKFORCE ou IMPEDIMENT. */
    @NotBlank(message = "Entry type is required")
    private String entryType;

    /** Responsavel pelo registro. Quando omitido, assume o usuario autenticado. */
    private Long responsibleUserId;

    @NotBlank(message = "Description is required")
    @Size(max = 2000, message = "Description must be at most 2000 characters")
    private String description;

    /** Anexo ja carregado na obra que documenta o registro (opcional). */
    private Long attachmentId;

    public DiaryEntryRequest() {}

    public DiaryEntryRequest(Instant entryDate, String entryType, Long responsibleUserId,
                             String description, Long attachmentId) {
        this.entryDate = entryDate;
        this.entryType = entryType;
        this.responsibleUserId = responsibleUserId;
        this.description = description;
        this.attachmentId = attachmentId;
    }

    public Instant getEntryDate() { return entryDate; }
    public void setEntryDate(Instant entryDate) { this.entryDate = entryDate; }

    public String getEntryType() { return entryType; }
    public void setEntryType(String entryType) { this.entryType = entryType; }

    public Long getResponsibleUserId() { return responsibleUserId; }
    public void setResponsibleUserId(Long responsibleUserId) { this.responsibleUserId = responsibleUserId; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Long getAttachmentId() { return attachmentId; }
    public void setAttachmentId(Long attachmentId) { this.attachmentId = attachmentId; }
}
