package br.pucpr.prissma_server.schedule;

public record ScheduleMemberResponse(
        Long constructionProjectId,
        Long userId,
        String userName,
        String userResponsibility
) {
    public static ScheduleMemberResponse from(ScheduleMember member) {
        return new ScheduleMemberResponse(
                member.getConstructionProject().getId(),
                member.getUser().getId(),
                member.getUser().getName(),
                member.getUserResponsibility()
        );
    }
}
