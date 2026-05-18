package br.pucpr.prissma_server.stage;

import br.pucpr.prissma_server.projects.ConstructionProject;

public class StageMapper {

    public static Stage toEntity(StageRequest request, ConstructionProject project) {
        Stage stage = new Stage();
        stage.setConstructionProject(project);
        stage.setName(request.getName());
        stage.setDescription(request.getDescription());
        stage.setDisplayOrder(request.getDisplayOrder());
        stage.setStatus(request.getStatus() != null ? request.getStatus() : "PLANNED");
        stage.setPlannedStartDate(request.getPlannedStartDate());
        stage.setPlannedEndDate(request.getPlannedEndDate());
        stage.setActualStartDate(request.getActualStartDate());
        stage.setActualEndDate(request.getActualEndDate());
        return stage;
    }

    public static void updateEntity(StageRequest request, Stage stage) {
        if (request.getName() != null && !request.getName().isBlank()) {
            stage.setName(request.getName());
        }
        if (request.getDescription() != null) {
            stage.setDescription(request.getDescription());
        }
        if (request.getDisplayOrder() != null) {
            stage.setDisplayOrder(request.getDisplayOrder());
        }
        if (request.getStatus() != null && !request.getStatus().isBlank()) {
            stage.setStatus(request.getStatus());
        }
        if (request.getPlannedStartDate() != null) {
            stage.setPlannedStartDate(request.getPlannedStartDate());
        }
        if (request.getPlannedEndDate() != null) {
            stage.setPlannedEndDate(request.getPlannedEndDate());
        }
        if (request.getActualStartDate() != null) {
            stage.setActualStartDate(request.getActualStartDate());
        }
        if (request.getActualEndDate() != null) {
            stage.setActualEndDate(request.getActualEndDate());
        }
    }

    public static StageResponse toResponse(Stage stage) {
        return new StageResponse(
            stage.getId(),
            stage.getConstructionProject().getId(),
            stage.getName(),
            stage.getDescription(),
            stage.getDisplayOrder(),
            stage.getStatus(),
            stage.getPlannedStartDate(),
            stage.getPlannedEndDate(),
            stage.getActualStartDate(),
            stage.getActualEndDate(),
            stage.getCreatedAt(),
            stage.getUpdatedAt()
        );
    }
}


