package br.pucpr.prissma_server.design;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface DesignSubmissionRepository extends JpaRepository<DesignSubmission, Long> {

    @Query("SELECT s FROM DesignSubmission s LEFT JOIN FETCH s.authorUser "
            + "WHERE s.proposal.id = :proposalId ORDER BY s.version DESC")
    List<DesignSubmission> findAllByProposalOrderByVersionDesc(@Param("proposalId") Long proposalId);

    /**
     * Versões de várias propostas de uma vez — é o que evita o N+1 da listagem,
     * onde cada card precisa da versão mais recente e da contagem.
     */
    @Query("SELECT s FROM DesignSubmission s WHERE s.proposal.id IN :proposalIds "
            + "ORDER BY s.proposal.id, s.version DESC")
    List<DesignSubmission> findAllByProposalIds(@Param("proposalIds") List<Long> proposalIds);

    Optional<DesignSubmission> findFirstByProposalIdOrderByVersionDesc(Long proposalId);

    @Query("SELECT COALESCE(MAX(s.version), 0) FROM DesignSubmission s WHERE s.proposal.id = :proposalId")
    int findMaxVersion(@Param("proposalId") Long proposalId);

    /**
     * Só as chaves dos arquivos, sem materializar as versões.
     *
     * É o que a exclusão da proposta precisa: carregar as entidades deixaria
     * filhas gerenciadas apontando para uma proposta que o {@code remove} acabou
     * de marcar, e o flush quebra com TransientObjectException.
     */
    @Query("SELECT s.fileUrl FROM DesignSubmission s "
            + "WHERE s.proposal.id = :proposalId AND s.fileUrl IS NOT NULL")
    List<String> findFileUrlsByProposal(@Param("proposalId") Long proposalId);
}
