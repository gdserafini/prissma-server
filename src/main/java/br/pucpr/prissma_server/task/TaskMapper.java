package br.pucpr.prissma_server.task;

public final class TaskMapper {

    private TaskMapper() {
    }

    public static TaskResponse toResponse(Task task) {
        if (task == null) {
            return null;
        }

        return new TaskResponse(
                task.getId(),
                task.getStage() != null ? task.getStage().getId() : null,
                task.getStage() != null ? task.getStage().getName() : null,
                task.getAssigneeUser() != null ? task.getAssigneeUser().getId() : null,
                task.getAssigneeUser() != null ? task.getAssigneeUser().getName() : null,
                task.getAssigneeUser() != null && task.getAssigneeUser().getRole() != null
                        ? task.getAssigneeUser().getRole().name() : null,
                task.getTitle(),
                task.getDescription(),
                task.getPriority(),
                task.getStatus(),
                task.getPlannedStartDate(),
                task.getPlannedEndDate(),
                task.getCompletedAt()
        );
    }
}

