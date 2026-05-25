package br.pucpr.prissma_server.budget;

import java.math.BigDecimal;

public class BudgetItemResponse {

    private Long id;
    private Long projectBudgetId;
    private String category;
    private String description;
    private BigDecimal plannedAmount;
    private BigDecimal totalSpent;
    private BigDecimal remaining;
    private boolean exceeded;

    public BudgetItemResponse() {}

    public BudgetItemResponse(Long id, Long projectBudgetId, String category, String description,
                              BigDecimal plannedAmount, BigDecimal totalSpent, BigDecimal remaining,
                              boolean exceeded) {
        this.id = id;
        this.projectBudgetId = projectBudgetId;
        this.category = category;
        this.description = description;
        this.plannedAmount = plannedAmount;
        this.totalSpent = totalSpent;
        this.remaining = remaining;
        this.exceeded = exceeded;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getProjectBudgetId() { return projectBudgetId; }
    public void setProjectBudgetId(Long projectBudgetId) { this.projectBudgetId = projectBudgetId; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public BigDecimal getPlannedAmount() { return plannedAmount; }
    public void setPlannedAmount(BigDecimal plannedAmount) { this.plannedAmount = plannedAmount; }

    public BigDecimal getTotalSpent() { return totalSpent; }
    public void setTotalSpent(BigDecimal totalSpent) { this.totalSpent = totalSpent; }

    public BigDecimal getRemaining() { return remaining; }
    public void setRemaining(BigDecimal remaining) { this.remaining = remaining; }

    public boolean isExceeded() { return exceeded; }
    public void setExceeded(boolean exceeded) { this.exceeded = exceeded; }
}
