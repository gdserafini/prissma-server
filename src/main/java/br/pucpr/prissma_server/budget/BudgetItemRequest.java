package br.pucpr.prissma_server.budget;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public class BudgetItemRequest {

    @NotBlank(message = "Category is required")
    @Size(max = 100, message = "Category must be at most 100 characters")
    private String category;

    @NotBlank(message = "Description is required")
    private String description;

    @NotNull(message = "Planned amount is required")
    @DecimalMin(value = "0.00", inclusive = true, message = "Planned amount must be zero or positive")
    @Digits(integer = 12, fraction = 2, message = "Planned amount must have at most 12 integer and 2 fraction digits")
    private BigDecimal plannedAmount;

    public BudgetItemRequest() {}

    public BudgetItemRequest(String category, String description, BigDecimal plannedAmount) {
        this.category = category;
        this.description = description;
        this.plannedAmount = plannedAmount;
    }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public BigDecimal getPlannedAmount() { return plannedAmount; }
    public void setPlannedAmount(BigDecimal plannedAmount) { this.plannedAmount = plannedAmount; }
}
