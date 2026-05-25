package br.pucpr.prissma_server.task;

import java.time.Instant;
import java.time.LocalDate;

public class TaskResponse {

    private Long id;
    private Long stageId;
    private String stageName;
    private Long assigneeUserId;
    private String assigneeName;
    private String assigneeRole;
    private String title;
    private String description;
    private String priority;
    private String status;
    private LocalDate plannedStartDate;
    private LocalDate plannedEndDate;
    private Instant completedAt;

    public TaskResponse() {
    }

    public TaskResponse(Long id, Long stageId, String stageName, Long assigneeUserId, String assigneeName,
                        String assigneeRole, String title, String description, String priority, String status,
                        LocalDate plannedStartDate, LocalDate plannedEndDate, Instant completedAt) {
        this.id = id;
        this.stageId = stageId;
        this.stageName = stageName;
        this.assigneeUserId = assigneeUserId;
        this.assigneeName = assigneeName;
        this.assigneeRole = assigneeRole;
        this.title = title;
        this.description = description;
        this.priority = priority;
        this.status = status;
        this.plannedStartDate = plannedStartDate;
        this.plannedEndDate = plannedEndDate;
        this.completedAt = completedAt;
    }

    public Long getId() {
        return id;
    }

    public Long getStageId() {
        return stageId;
    }

    public String getStageName() {
        return stageName;
    }

    public Long getAssigneeUserId() {
        return assigneeUserId;
    }

    public String getAssigneeName() {
        return assigneeName;
    }

    public String getAssigneeRole() {
        return assigneeRole;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getPriority() {
        return priority;
    }

    public String getStatus() {
        return status;
    }

    public LocalDate getPlannedStartDate() {
        return plannedStartDate;
    }

    public LocalDate getPlannedEndDate() {
        return plannedEndDate;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }
}

