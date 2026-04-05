package br.pucpr.prissma_server.projects;

import br.pucpr.prissma_server.users.User;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "construction_project_members", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"construction_project_id", "user_id"})
})
public class ConstructionProjectMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "construction_project_id", nullable = false)
    private ConstructionProject constructionProject;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "role_in_project", nullable = false, length = 30)
    private String roleInProject;

    @Column(name = "membership_status", nullable = false, length = 20)
    private String membershipStatus = "ACTIVE";

    @Column(name = "joined_at", nullable = false)
    private Instant joinedAt;

    public ConstructionProjectMember() {
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

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getRoleInProject() {
        return roleInProject;
    }

    public void setRoleInProject(String roleInProject) {
        this.roleInProject = roleInProject;
    }

    public String getMembershipStatus() {
        return membershipStatus;
    }

    public void setMembershipStatus(String membershipStatus) {
        this.membershipStatus = membershipStatus;
    }

    public Instant getJoinedAt() {
        return joinedAt;
    }

    public void setJoinedAt(Instant joinedAt) {
        this.joinedAt = joinedAt;
    }
}

