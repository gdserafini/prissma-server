package br.pucpr.prissma_server.design;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DesignProposalRepository extends JpaRepository<DesignProposal, Long> {

    @Query(value = "SELECT p FROM DesignProposal p "
            + "LEFT JOIN FETCH p.createdByUser "
            + "LEFT JOIN FETCH p.stage "
            + "WHERE p.constructionProject.id = :projectId "
            + "ORDER BY p.updatedAt DESC, p.id DESC",
            countQuery = "SELECT COUNT(p) FROM DesignProposal p WHERE p.constructionProject.id = :projectId")
    Page<DesignProposal> findPageByProject(@Param("projectId") Long projectId, Pageable pageable);

    boolean existsByConstructionProjectIdAndTitleIgnoreCase(Long projectId, String title);

    boolean existsByConstructionProjectIdAndTitleIgnoreCaseAndIdNot(Long projectId, String title, Long id);
}
