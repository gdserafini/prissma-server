package br.pucpr.prissma_server.projects;

import java.time.LocalDate;

public class AcompanhamentoTaskResponse {

    private Long id;
    private String title;
    private String description;
    private String priority;
    private String status;
    private LocalDate plannedStartDate;
    private LocalDate plannedEndDate;

    public AcompanhamentoTaskResponse() {
    }

    public AcompanhamentoTaskResponse(Long id, String title, String description,
                                      String priority, String status,
                                      LocalDate plannedStartDate, LocalDate plannedEndDate) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.priority = priority;
        this.status = status;
        this.plannedStartDate = plannedStartDate;
        this.plannedEndDate = plannedEndDate;
    }

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getPriority() { return priority; }
    public String getStatus() { return status; }
    public LocalDate getPlannedStartDate() { return plannedStartDate; }
    public LocalDate getPlannedEndDate() { return plannedEndDate; }
}
