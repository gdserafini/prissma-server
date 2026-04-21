package br.pucpr.prissma_server.projects;

import java.time.LocalDate;
import java.util.List;

public class AcompanhamentoStageResponse {

    private Long id;
    private String name;
    private String description;
    private Integer displayOrder;
    private String status;
    private LocalDate plannedStartDate;
    private LocalDate plannedEndDate;
    private List<AcompanhamentoTaskResponse> tasks;

    public AcompanhamentoStageResponse() {
    }

    public AcompanhamentoStageResponse(Long id, String name, String description,
                                       Integer displayOrder, String status,
                                       LocalDate plannedStartDate, LocalDate plannedEndDate,
                                       List<AcompanhamentoTaskResponse> tasks) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.displayOrder = displayOrder;
        this.status = status;
        this.plannedStartDate = plannedStartDate;
        this.plannedEndDate = plannedEndDate;
        this.tasks = tasks;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public Integer getDisplayOrder() { return displayOrder; }
    public String getStatus() { return status; }
    public LocalDate getPlannedStartDate() { return plannedStartDate; }
    public LocalDate getPlannedEndDate() { return plannedEndDate; }
    public List<AcompanhamentoTaskResponse> getTasks() { return tasks; }
}
