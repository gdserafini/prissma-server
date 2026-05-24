package br.pucpr.prissma_server.budget;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public class ExpenseResponse {

    private Long id;
    private Long budgetItemId;
    private Long stageId;
    private String description;
    private BigDecimal amount;
    private String supplier;
    private String receiptUrl;
    private LocalDate spentAt;
    private Instant createdAt;
    private boolean categoryExceeded;
    private boolean budgetExceeded;

    public ExpenseResponse() {}

    public ExpenseResponse(Long id, Long budgetItemId, Long stageId, String description,
                           BigDecimal amount, String supplier, String receiptUrl,
                           LocalDate spentAt, Instant createdAt,
                           boolean categoryExceeded, boolean budgetExceeded) {
        this.id = id;
        this.budgetItemId = budgetItemId;
        this.stageId = stageId;
        this.description = description;
        this.amount = amount;
        this.supplier = supplier;
        this.receiptUrl = receiptUrl;
        this.spentAt = spentAt;
        this.createdAt = createdAt;
        this.categoryExceeded = categoryExceeded;
        this.budgetExceeded = budgetExceeded;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getBudgetItemId() { return budgetItemId; }
    public void setBudgetItemId(Long budgetItemId) { this.budgetItemId = budgetItemId; }

    public Long getStageId() { return stageId; }
    public void setStageId(Long stageId) { this.stageId = stageId; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getSupplier() { return supplier; }
    public void setSupplier(String supplier) { this.supplier = supplier; }

    public String getReceiptUrl() { return receiptUrl; }
    public void setReceiptUrl(String receiptUrl) { this.receiptUrl = receiptUrl; }

    public LocalDate getSpentAt() { return spentAt; }
    public void setSpentAt(LocalDate spentAt) { this.spentAt = spentAt; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public boolean isCategoryExceeded() { return categoryExceeded; }
    public void setCategoryExceeded(boolean categoryExceeded) { this.categoryExceeded = categoryExceeded; }

    public boolean isBudgetExceeded() { return budgetExceeded; }
    public void setBudgetExceeded(boolean budgetExceeded) { this.budgetExceeded = budgetExceeded; }
}
