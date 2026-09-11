package br.pucpr.prissma_server.design;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EnvironmentPreviewRepository extends JpaRepository<EnvironmentPreview, Long> {

    Optional<EnvironmentPreview> findByIdAndProposalId(Long id, Long proposalId);
}
