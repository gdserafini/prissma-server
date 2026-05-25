package br.pucpr.prissma_server.budget;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public class ProjectBudgetRequest {

    private String description;

    @NotNull(message = "Planned total is required")
    @DecimalMin(value = "0.00", inclusive = true, message = "Planned total must be zero or positive")
    @Digits(integer = 12, fraction = 2, message = "Planned total must have at most 12 integer and 2 fraction digits")
    private BigDecimal plannedTotal;

    public ProjectBudgetRequest() {}

    public ProjectBudgetRequest(String description, BigDecimal plannedTotal) {
        this.description = description;
        this.plannedTotal = plannedTotal;
    }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public BigDecimal getPlannedTotal() { return plannedTotal; }
    public void setPlannedTotal(BigDecimal plannedTotal) { this.plannedTotal = plannedTotal; }
}
