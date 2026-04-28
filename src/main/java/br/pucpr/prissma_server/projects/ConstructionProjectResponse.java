package br.pucpr.prissma_server.projects;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record ConstructionProjectResponse(
        Long id,
        String title,
        String cep,
        String street,
        String city,
        String state,
        String number,
        String complement,
        String projectType,
        String category,
        BigDecimal landArea,
        BigDecimal builtArea,
        String status,
        LocalDate plannedStartDate,
        LocalDate plannedEndDate,
        Instant createdAt,
        Instant updatedAt
) {
    public static ConstructionProjectResponse from(ConstructionProject p) {
        return new ConstructionProjectResponse(
                p.getId(),
                p.getTitle(),
                p.getCep(),
                p.getStreet(),
                p.getCity(),
                p.getState(),
                p.getNumber(),
                p.getComplement(),
                p.getProjectType(),
                p.getCategory(),
                p.getLandArea(),
                p.getBuiltArea(),
                p.getStatus(),
                p.getPlannedStartDate(),
                p.getPlannedEndDate(),
                p.getCreatedAt(),
                p.getUpdatedAt()
        );
    }
}

