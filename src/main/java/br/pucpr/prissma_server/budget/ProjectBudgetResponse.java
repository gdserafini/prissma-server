package br.pucpr.prissma_server.budget;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public class ProjectBudgetResponse {

    private Long id;
    private Long constructionProjectId;
    private String description;
    private BigDecimal plannedTotal;
    private BigDecimal totalSpent;
    private BigDecimal remaining;
    private boolean exceeded;
    private Instant createdAt;
    private Instant updatedAt;
    private List<BudgetItemResponse> items;

    public ProjectBudgetResponse() {}

    public ProjectBudgetResponse(Long id, Long constructionProjectId, String description,
                                 BigDecimal plannedTotal, BigDecimal totalSpent, BigDecimal remaining,
                                 boolean exceeded, Instant createdAt, Instant updatedAt,
                                 List<BudgetItemResponse> items) {
        this.id = id;
        this.constructionProjectId = constructionProjectId;
        this.description = description;
        this.plannedTotal = plannedTotal;
        this.totalSpent = totalSpent;
        this.remaining = remaining;
        this.exceeded = exceeded;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.items = items;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getConstructionProjectId() { return constructionProjectId; }
    public void setConstructionProjectId(Long constructionProjectId) { this.constructionProjectId = constructionProjectId; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public BigDecimal getPlannedTotal() { return plannedTotal; }
    public void setPlannedTotal(BigDecimal plannedTotal) { this.plannedTotal = plannedTotal; }

    public BigDecimal getTotalSpent() { return totalSpent; }
    public void setTotalSpent(BigDecimal totalSpent) { this.totalSpent = totalSpent; }

    public BigDecimal getRemaining() { return remaining; }
    public void setRemaining(BigDecimal remaining) { this.remaining = remaining; }

    public boolean isExceeded() { return exceeded; }
    public void setExceeded(boolean exceeded) { this.exceeded = exceeded; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    public List<BudgetItemResponse> getItems() { return items; }
    public void setItems(List<BudgetItemResponse> items) { this.items = items; }
}
