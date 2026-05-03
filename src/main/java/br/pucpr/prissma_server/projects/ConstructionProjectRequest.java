package br.pucpr.prissma_server.projects;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ConstructionProjectRequest(
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
        LocalDate plannedEndDate
) {
    public ConstructionProject toEntity() {
        ConstructionProject p = new ConstructionProject();
        p.setTitle(this.title);
        p.setCep(this.cep);
        p.setStreet(this.street);
        p.setCity(this.city);
        p.setState(this.state);
        p.setNumber(this.number);
        p.setComplement(this.complement);
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

