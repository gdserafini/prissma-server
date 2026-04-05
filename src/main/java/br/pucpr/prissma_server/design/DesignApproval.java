package br.pucpr.prissma_server.design;

import br.pucpr.prissma_server.users.User;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "design_approvals")
public class DesignApproval {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "design_submission_id", nullable = false)
    private DesignSubmission designSubmission;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approver_user_id", nullable = false)
    private User approverUser;

    @Column(name = "approval_status", nullable = false, length = 20)
    private String approvalStatus;

    @Column(columnDefinition = "TEXT")
    private String comment;

    @Column(name = "approved_at")
    private Instant approvedAt;

    public DesignApproval() {
    }

    public Long getId() {
        return id;
    }

    public DesignSubmission getDesignSubmission() {
        return designSubmission;
    }

    public void setDesignSubmission(DesignSubmission designSubmission) {
        this.designSubmission = designSubmission;
    }

    public User getApproverUser() {
        return approverUser;
    }

    public void setApproverUser(User approverUser) {
        this.approverUser = approverUser;
    }

    public String getApprovalStatus() {
        return approvalStatus;
    }

    public void setApprovalStatus(String approvalStatus) {
        this.approvalStatus = approvalStatus;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public Instant getApprovedAt() {
        return approvedAt;
    }

    public void setApprovedAt(Instant approvedAt) {
        this.approvedAt = approvedAt;
    }
}

