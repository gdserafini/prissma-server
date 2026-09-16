package br.pucpr.prissma_server.schedule;

import br.pucpr.prissma_server.projects.ConstructionProject;
import br.pucpr.prissma_server.users.User;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "schedule_allocations", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"construction_project_id", "user_id", "allocation_date"})
})
public class ScheduleAllocation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "construction_project_id", nullable = false)
    private ConstructionProject constructionProject;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "allocation_date", nullable = false)
    private LocalDate allocationDate;

    @Column(nullable = false, precision = 4, scale = 2)
    private BigDecimal hours;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public ScheduleAllocation() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public ConstructionProject getConstructionProject() {
        return constructionProject;
    }

    public void setConstructionProject(ConstructionProject constructionProject) {
        this.constructionProject = constructionProject;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public LocalDate getAllocationDate() {
        return allocationDate;
    }

    public void setAllocationDate(LocalDate allocationDate) {
        this.allocationDate = allocationDate;
    }

    public BigDecimal getHours() {
        return hours;
    }

    public void setHours(BigDecimal hours) {
        this.hours = hours;
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
