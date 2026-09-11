package br.pucpr.prissma_server.design;

import br.pucpr.prissma_server.genai.EnvironmentPreviewRequest;

import java.time.Instant;
import java.util.List;

/**
 * A proposta como a tela precisa dela.
 *
 * {@code latestVersion} e {@code versionCount} vêm preenchidos na listagem para
 * que o card não precise de uma requisição por proposta; {@code versions} só é
 * preenchido no detalhe.
 */
public record ProposalResponse(
        Long id,
        Long constructionProjectId,
        Long stageId,
        String title,
        String description,
        EnvironmentPreviewRequest.EnvironmentType environmentType,
        Long createdByUserId,
        String createdByName,
        ProposalVersionResponse latestVersion,
        int versionCount,
        List<ProposalVersionResponse> versions,
        Instant createdAt,
        Instant updatedAt
) {

    public static ProposalResponse card(DesignProposal proposal,
                                        DesignSubmission latestVersion,
                                        int versionCount) {
        return build(proposal, latestVersion, versionCount, null);
    }

    public static ProposalResponse detail(DesignProposal proposal, List<DesignSubmission> versions) {
        DesignSubmission latest = versions.isEmpty() ? null : versions.getFirst();
        return build(proposal, latest, versions.size(),
                versions.stream().map(ProposalVersionResponse::from).toList());
    }

    private static ProposalResponse build(DesignProposal proposal,
                                          DesignSubmission latestVersion,
                                          int versionCount,
                                          List<ProposalVersionResponse> versions) {
        return new ProposalResponse(
                proposal.getId(),
                proposal.getConstructionProject() != null ? proposal.getConstructionProject().getId() : null,
                proposal.getStage() != null ? proposal.getStage().getId() : null,
                proposal.getTitle(),
                proposal.getDescription(),
                proposal.getEnvironmentType(),
                proposal.getCreatedByUser() != null ? proposal.getCreatedByUser().getId() : null,
                proposal.getCreatedByName(),
                latestVersion != null ? ProposalVersionResponse.from(latestVersion) : null,
                versionCount,
                versions,
                proposal.getCreatedAt(),
                proposal.getUpdatedAt()
        );
    }
}
