package br.pucpr.prissma_server.design;

import br.pucpr.prissma_server.users.User;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

/**
 * Um pedido de prévia visual por IA.
 *
 * A chamada à OpenAI leva de 30 a 60s, então o disparo só grava esta linha em
 * {@code PROCESSING} e devolve 202; quem conclui é o job assíncrono, que ao
 * final aponta {@link #resultSubmission} para a versão recém-criada.
 */
@Entity
@Table(name = "ai_environment_previews")
public class EnvironmentPreview {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proposal_id", nullable = false)
    private DesignProposal proposal;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PreviewStatus status = PreviewStatus.PROCESSING;

    /** Chaves no {@code FileStorageService} — as imagens de entrada não viram anexos da obra. */
    @Column(name = "raw_image_key", nullable = false, columnDefinition = "TEXT")
    private String rawImageKey;

    @Column(name = "floor_plan_key", columnDefinition = "TEXT")
    private String floorPlanKey;

    /** As escolhas do usuário como vieram, para o retry repetir a mesma geração. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "options_json", nullable = false, columnDefinition = "jsonb")
    private String optionsJson;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "result_submission_id")
    private DesignSubmission resultSubmission;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requested_by_user_id")
    private User requestedByUser;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    public EnvironmentPreview() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public DesignProposal getProposal() {
        return proposal;
    }

    public void setProposal(DesignProposal proposal) {
        this.proposal = proposal;
    }

    public PreviewStatus getStatus() {
        return status;
    }

    public void setStatus(PreviewStatus status) {
        this.status = status;
    }

    public String getRawImageKey() {
        return rawImageKey;
    }

    public void setRawImageKey(String rawImageKey) {
        this.rawImageKey = rawImageKey;
    }

    public String getFloorPlanKey() {
        return floorPlanKey;
    }

    public void setFloorPlanKey(String floorPlanKey) {
        this.floorPlanKey = floorPlanKey;
    }

    public String getOptionsJson() {
        return optionsJson;
    }

    public void setOptionsJson(String optionsJson) {
        this.optionsJson = optionsJson;
    }

    public DesignSubmission getResultSubmission() {
        return resultSubmission;
    }

    public void setResultSubmission(DesignSubmission resultSubmission) {
        this.resultSubmission = resultSubmission;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public User getRequestedByUser() {
        return requestedByUser;
    }

    public void setRequestedByUser(User requestedByUser) {
        this.requestedByUser = requestedByUser;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
    }
}
