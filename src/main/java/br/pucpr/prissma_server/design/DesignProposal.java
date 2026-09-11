package br.pucpr.prissma_server.design;

import br.pucpr.prissma_server.genai.EnvironmentPreviewRequest;
import br.pucpr.prissma_server.projects.ConstructionProject;
import br.pucpr.prissma_server.stage.Stage;
import br.pucpr.prissma_server.users.User;
import jakarta.persistence.*;

import java.time.Instant;

/**
 * Proposta de design de um ambiente da obra — o card da tela de Propostas.
 *
 * O conteúdo em si mora nas versões ({@link DesignSubmission}): a proposta só
 * guarda o que não muda de uma versão para a outra (ambiente, título, obra).
 */
@Entity
@Table(name = "design_proposals", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"construction_project_id", "title"})
})
public class DesignProposal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "construction_project_id", nullable = false)
    private ConstructionProject constructionProject;

    /** Etapa do cronograma, quando a proposta está amarrada a uma. Opcional. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stage_id")
    private Stage stage;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    /** Mesmo enum que o prompt da IA usa, para não existirem duas listas de ambientes. */
    @Enumerated(EnumType.STRING)
    @Column(name = "environment_type", nullable = false, length = 20)
    private EnvironmentPreviewRequest.EnvironmentType environmentType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_user_id")
    private User createdByUser;

    @Column(name = "created_by_name", nullable = false)
    private String createdByName;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public DesignProposal() {
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

    public Stage getStage() {
        return stage;
    }

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public EnvironmentPreviewRequest.EnvironmentType getEnvironmentType() {
        return environmentType;
    }

    public void setEnvironmentType(EnvironmentPreviewRequest.EnvironmentType environmentType) {
        this.environmentType = environmentType;
    }

    public User getCreatedByUser() {
        return createdByUser;
    }

    public void setCreatedByUser(User createdByUser) {
        this.createdByUser = createdByUser;
    }

    public String getCreatedByName() {
        return createdByName;
    }

    public void setCreatedByName(String createdByName) {
        this.createdByName = createdByName;
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
