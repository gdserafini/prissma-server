package br.pucpr.prissma_server.budget;

import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
@Table(name = "budget_items")
public class BudgetItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_budget_id", nullable = false)
    private ProjectBudget projectBudget;

    @Column(nullable = false, length = 100)
    private String category;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(name = "planned_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal plannedAmount;

    public BudgetItem() {
    }

    public Long getId() {
        return id;
    }

    public ProjectBudget getProjectBudget() {
        return projectBudget;
    }

    public void setProjectBudget(ProjectBudget projectBudget) {
        this.projectBudget = projectBudget;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getPlannedAmount() {
        return plannedAmount;
    }

    public void setPlannedAmount(BigDecimal plannedAmount) {
        this.plannedAmount = plannedAmount;
    }
}


