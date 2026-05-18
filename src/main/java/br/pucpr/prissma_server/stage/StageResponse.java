package br.pucpr.prissma_server.stage;

import java.time.Instant;
import java.time.LocalDate;

public class StageResponse {

    private Long id;
    private Long constructionProjectId;
    private String name;
    private String description;
    private Integer displayOrder;
    private String status;
    private LocalDate plannedStartDate;
    private LocalDate plannedEndDate;
    private LocalDate actualStartDate;
    private LocalDate actualEndDate;
    private String pavimento;
    private Instant createdAt;
    private Instant updatedAt;

    public StageResponse() {}

    public StageResponse(Long id, Long constructionProjectId, String name, String description,
                        Integer displayOrder, String status, LocalDate plannedStartDate,
                        LocalDate plannedEndDate, LocalDate actualStartDate, LocalDate actualEndDate,
                        String pavimento, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.constructionProjectId = constructionProjectId;
        this.name = name;
        this.description = description;
        this.displayOrder = displayOrder;
        this.status = status;
        this.plannedStartDate = plannedStartDate;
        this.plannedEndDate = plannedEndDate;
        this.actualStartDate = actualStartDate;
        this.actualEndDate = actualEndDate;
        this.pavimento = pavimento;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getConstructionProjectId() { return constructionProjectId; }
    public void setConstructionProjectId(Long constructionProjectId) { this.constructionProjectId = constructionProjectId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Integer getDisplayOrder() { return displayOrder; }
    public void setDisplayOrder(Integer displayOrder) { this.displayOrder = displayOrder; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDate getPlannedStartDate() { return plannedStartDate; }
    public void setPlannedStartDate(LocalDate plannedStartDate) { this.plannedStartDate = plannedStartDate; }

    public LocalDate getPlannedEndDate() { return plannedEndDate; }
    public void setPlannedEndDate(LocalDate plannedEndDate) { this.plannedEndDate = plannedEndDate; }

    public LocalDate getActualStartDate() { return actualStartDate; }
    public void setActualStartDate(LocalDate actualStartDate) { this.actualStartDate = actualStartDate; }

    public LocalDate getActualEndDate() { return actualEndDate; }
    public void setActualEndDate(LocalDate actualEndDate) { this.actualEndDate = actualEndDate; }

    public String getPavimento() { return pavimento; }
    public void setPavimento(String pavimento) { this.pavimento = pavimento; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}

