package br.pucpr.prissma_server.projects;

import jakarta.persistence.*;

import java.time.Instant;

/**
 * Permissão concedida a um papel dentro de uma obra específica. A existência de
 * linhas para um par (obra, papel) sobrescreve as permissões padrão do papel,
 * permitindo a customização exigida pelo RF5 (critério 3).
 */
@Entity
@Table(name = "project_role_permissions", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"construction_project_id", "role_in_project", "permission"})
})
public class ProjectRolePermission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "construction_project_id", nullable = false)
    private ConstructionProject constructionProject;

    @Enumerated(EnumType.STRING)
    @Column(name = "role_in_project", nullable = false, length = 30)
    private ProjectRole role;

    @Enumerated(EnumType.STRING)
    @Column(name = "permission", nullable = false, length = 40)
    private ProjectPermission permission;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public ProjectRolePermission() {
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

    public ProjectRole getRole() {
        return role;
    }

    public void setRole(ProjectRole role) {
        this.role = role;
    }

    public ProjectPermission getPermission() {
        return permission;
    }

    public void setPermission(ProjectPermission permission) {
        this.permission = permission;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
