package br.pucpr.prissma_server.diary;

import jakarta.validation.constraints.Size;

import java.time.Instant;

/**
 * PATCH do registro de diario: todo campo e opcional e os nulos sao ignorados,
 * seguindo o mesmo estilo de atualizacao parcial de etapas e tarefas.
 */
public class DiaryEntryUpdateRequest {

    private Instant entryDate;

    private String entryType;

    private Long responsibleUserId;

    @Size(max = 2000, message = "Description must be at most 2000 characters")
    private String description;

    private Long attachmentId;

    /** Desvincula o anexo atual; ignorado quando attachmentId vem preenchido. */
    private Boolean unlinkAttachment;

    public DiaryEntryUpdateRequest() {}

    public DiaryEntryUpdateRequest(Instant entryDate, String entryType, Long responsibleUserId,
                                   String description, Long attachmentId, Boolean unlinkAttachment) {
        this.entryDate = entryDate;
        this.entryType = entryType;
        this.responsibleUserId = responsibleUserId;
        this.description = description;
        this.attachmentId = attachmentId;
        this.unlinkAttachment = unlinkAttachment;
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

    public Boolean getUnlinkAttachment() { return unlinkAttachment; }
    public void setUnlinkAttachment(Boolean unlinkAttachment) { this.unlinkAttachment = unlinkAttachment; }
}
