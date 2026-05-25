package br.pucpr.prissma_server.projects;

import br.pucpr.prissma_server.stage.Stage;
import br.pucpr.prissma_server.task.Task;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class AcompanhamentoStageResponse {

    private Long id;
    private String name;
    private String description;
    private Integer displayOrder;
    private String status;
    private LocalDate plannedStartDate;
    private LocalDate plannedEndDate;
    private int totalTarefas;
    private Map<String, Long> taskStatusCounts;

    public AcompanhamentoStageResponse() {
    }

    public AcompanhamentoStageResponse(Long id, String name, String description,
                                       Integer displayOrder, String status,
                                       LocalDate plannedStartDate, LocalDate plannedEndDate,
                                       int totalTarefas, Map<String, Long> taskStatusCounts) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.displayOrder = displayOrder;
        this.status = status;
        this.plannedStartDate = plannedStartDate;
        this.plannedEndDate = plannedEndDate;
        this.totalTarefas = totalTarefas;
        this.taskStatusCounts = taskStatusCounts;
    }

    public static AcompanhamentoStageResponse from(Stage stage, List<Task> tasks) {
        Map<String, Long> taskStatusCounts = new LinkedHashMap<>();
        taskStatusCounts.put("TODO", 0L);
        taskStatusCounts.put("IN_PROGRESS", 0L);
        taskStatusCounts.put("BLOCKED", 0L);
        taskStatusCounts.put("DONE", 0L);
        for (Task task : tasks) {
            String status = task.getStatus();
            if (status != null) {
                taskStatusCounts.put(status, taskStatusCounts.getOrDefault(status, 0L) + 1L);
            }
        }

        return new AcompanhamentoStageResponse(
                stage.getId(),
                stage.getName(),
                stage.getDescription(),
                stage.getDisplayOrder(),
                stage.getStatus(),
                stage.getPlannedStartDate(),
                stage.getPlannedEndDate(),
                tasks.size(),
                taskStatusCounts
        );
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public Integer getDisplayOrder() { return displayOrder; }
    public String getStatus() { return status; }
    public LocalDate getPlannedStartDate() { return plannedStartDate; }
    public LocalDate getPlannedEndDate() { return plannedEndDate; }
    public int getTotalTarefas() { return totalTarefas; }
    public Map<String, Long> getTaskStatusCounts() { return taskStatusCounts; }
}
