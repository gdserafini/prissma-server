package br.pucpr.prissma_server.projects;

import br.pucpr.prissma_server.users.UserResponse;

import java.time.Instant;

public record ConstructionProjectMemberResponse(
        Long id,
        Long projectId,
        UserResponse user,
        String roleInProject,
        String membershipStatus,
        Instant joinedAt
) {
    public static ConstructionProjectMemberResponse from(ConstructionProjectMember member) {
        return new ConstructionProjectMemberResponse(
                member.getId(),
                member.getConstructionProject().getId(),
                UserResponse.from(member.getUser()),
                member.getRoleInProject(),
                member.getMembershipStatus(),
                member.getJoinedAt()
        );
    }
}

