package br.pucpr.prissma_server.schedule;

import br.pucpr.prissma_server.task.Task;

import java.time.LocalDate;

public record ScheduledTaskResponse(
        Long id,
        String title,
        String status,
        String priority,
        Long stageId,
        String stageName,
        LocalDate plannedStartDate,
        LocalDate plannedEndDate
) {
    public static ScheduledTaskResponse from(Task task) {
        return new ScheduledTaskResponse(
                task.getId(),
                task.getTitle(),
                task.getStatus(),
                task.getPriority(),
                task.getStage() != null ? task.getStage().getId() : null,
                task.getStage() != null ? task.getStage().getName() : null,
                task.getPlannedStartDate(),
                task.getPlannedEndDate()
        );
    }
}
