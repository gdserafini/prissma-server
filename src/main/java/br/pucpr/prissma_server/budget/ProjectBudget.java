package br.pucpr.prissma_server.budget;

import br.pucpr.prissma_server.projects.ConstructionProject;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "project_budgets")
public class ProjectBudget {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "construction_project_id", nullable = false, unique = true)
    private ConstructionProject constructionProject;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "planned_total", nullable = false, precision = 14, scale = 2)
    private BigDecimal plannedTotal;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public ProjectBudget() {
    }

    public Long getId() {
        return id;
    }

    public ConstructionProject getConstructionProject() {
        return constructionProject;
    }

    public void setConstructionProject(ConstructionProject constructionProject) {
        this.constructionProject = constructionProject;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getPlannedTotal() {
        return plannedTotal;
    }

    public void setPlannedTotal(BigDecimal plannedTotal) {
        this.plannedTotal = plannedTotal;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}


