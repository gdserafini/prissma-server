package br.pucpr.prissma_server.projects;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ConstructionProjectRequest(
        String title,
        String address,
        String projectType,
        String category,
        BigDecimal landArea,
        BigDecimal builtArea,
        String status,
        LocalDate plannedStartDate,
        LocalDate plannedEndDate
) {
    public ConstructionProject toEntity() {
        ConstructionProject p = new ConstructionProject();
        p.setTitle(this.title);
        p.setAddress(this.address);
        p.setProjectType(this.projectType);
        p.setCategory(this.category);
        p.setLandArea(this.landArea);
        p.setBuiltArea(this.builtArea);
        if (this.status != null) p.setStatus(this.status);
        p.setPlannedStartDate(this.plannedStartDate);
        p.setPlannedEndDate(this.plannedEndDate);
        return p;
    }
}

