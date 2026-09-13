package br.pucpr.prissma_server.design;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class ProposalRequest {

    @NotBlank(message = "O título é obrigatório")
    @Size(max = 255, message = "O título deve ter no máximo 255 caracteres")
    private String title;

    @Size(max = 2000, message = "A descrição deve ter no máximo 2000 caracteres")
    private String description;

    @NotBlank(message = "O ambiente é obrigatório")
    private String environmentType;

    /** Etapa do cronograma. Opcional — a proposta é da obra. */
    private Long stageId;

    public ProposalRequest() {
    }

    public ProposalRequest(String title, String description, String environmentType, Long stageId) {
        this.title = title;
        this.description = description;
        this.environmentType = environmentType;
        this.stageId = stageId;
    }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getEnvironmentType() { return environmentType; }
    public void setEnvironmentType(String environmentType) { this.environmentType = environmentType; }

    public Long getStageId() { return stageId; }
    public void setStageId(Long stageId) { this.stageId = stageId; }
}
