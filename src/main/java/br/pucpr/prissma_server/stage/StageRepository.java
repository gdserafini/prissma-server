package br.pucpr.prissma_server.stage;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface StageRepository extends JpaRepository<Stage, Long> {

    List<Stage> findByConstructionProjectIdOrderByDisplayOrder(Long constructionProjectId);

    Optional<Stage> findByConstructionProjectIdAndDisplayOrder(Long constructionProjectId, Integer displayOrder);

    @Query("SELECT COUNT(s) FROM Stage s WHERE s.constructionProject.id = :projectId AND s.displayOrder = :displayOrder")
    int countByProjectAndDisplayOrder(@Param("projectId") Long projectId, @Param("displayOrder") Integer displayOrder);
}

