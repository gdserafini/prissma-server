package br.pucpr.prissma_server.budget;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public class ExpenseRequest {

    private Long stageId;

    @NotBlank(message = "Description is required")
    private String description;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", inclusive = true, message = "Amount must be positive")
    @Digits(integer = 12, fraction = 2, message = "Amount must have at most 12 integer and 2 fraction digits")
    private BigDecimal amount;

    @Size(max = 255, message = "Supplier must be at most 255 characters")
    private String supplier;

    private String receiptUrl;

    @NotNull(message = "Spent at date is required")
    private LocalDate spentAt;

    public ExpenseRequest() {}

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
}
