package br.pucpr.prissma_server.task;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TaskRepository extends JpaRepository<Task, Long> {

    @Query("SELECT t FROM Task t LEFT JOIN FETCH t.stage LEFT JOIN FETCH t.assigneeUser WHERE t.stage.id = :stageId ORDER BY t.createdAt ASC, t.id ASC")
    List<Task> findByStageIdOrderByCreatedAtAscIdAsc(@Param("stageId") Long stageId);

    @Query("SELECT t FROM Task t LEFT JOIN FETCH t.stage LEFT JOIN FETCH t.assigneeUser WHERE t.stage.constructionProject.id = :projectId ORDER BY t.createdAt ASC, t.id ASC")
    List<Task> findByStageConstructionProjectIdOrderByCreatedAtAscIdAsc(@Param("projectId") Long projectId);

    @Query("SELECT t FROM Task t LEFT JOIN FETCH t.stage LEFT JOIN FETCH t.assigneeUser WHERE t.id = :id AND t.stage.id = :stageId")
    Optional<Task> findByIdAndStageId(@Param("id") Long id, @Param("stageId") Long stageId);
}


