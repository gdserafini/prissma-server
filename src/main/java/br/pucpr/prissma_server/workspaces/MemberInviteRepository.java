package br.pucpr.prissma_server.workspaces;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MemberInviteRepository extends JpaRepository<MemberInvite, Long> {

    Optional<MemberInvite> findByTokenHashAndDeletedAtIsNull(String tokenHash);

    Optional<MemberInvite> findByWorkspaceIdAndInvitedEmailAndAcceptedFalseAndDeletedAtIsNull(
            Long workspaceId, String invitedEmail);
}
