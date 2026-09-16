package br.pucpr.prissma_server.task;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface TaskRepository extends JpaRepository<Task, Long> {

    @Query("SELECT t FROM Task t LEFT JOIN FETCH t.stage LEFT JOIN FETCH t.assigneeUser WHERE t.stage.id = :stageId ORDER BY t.createdAt ASC, t.id ASC")
    List<Task> findByStageIdOrderByCreatedAtAscIdAsc(@Param("stageId") Long stageId);

    @Query("SELECT t FROM Task t LEFT JOIN FETCH t.stage LEFT JOIN FETCH t.assigneeUser WHERE t.assigneeUser.id = :userId ORDER BY t.createdAt ASC, t.id ASC")
    List<Task> findByAssigneeUserIdOrderByCreatedAtAscIdAsc(@Param("userId") Long userId);

    @Query("SELECT t FROM Task t LEFT JOIN FETCH t.stage LEFT JOIN FETCH t.assigneeUser WHERE t.stage.constructionProject.id = :projectId ORDER BY t.createdAt ASC, t.id ASC")
    List<Task> findByStageConstructionProjectIdOrderByCreatedAtAscIdAsc(@Param("projectId") Long projectId);

    /**
     * Tarefas com responsável cujo período planejado cruza [startDate, endDate].
     * Tarefa com só uma das datas conta como tarefa de um dia; sem nenhuma, fica de fora.
     */
    @Query("SELECT t FROM Task t JOIN FETCH t.stage s JOIN FETCH t.assigneeUser " +
            "WHERE s.constructionProject.id = :projectId " +
            "AND COALESCE(t.plannedStartDate, t.plannedEndDate) <= :endDate " +
            "AND COALESCE(t.plannedEndDate, t.plannedStartDate) >= :startDate " +
            "ORDER BY COALESCE(t.plannedStartDate, t.plannedEndDate) ASC, t.id ASC")
    List<Task> findAssignedInProjectDuringPeriod(@Param("projectId") Long projectId,
                                                 @Param("startDate") LocalDate startDate,
                                                 @Param("endDate") LocalDate endDate);

    @Query("SELECT t FROM Task t LEFT JOIN FETCH t.stage LEFT JOIN FETCH t.assigneeUser WHERE t.id = :id AND t.stage.id = :stageId")
    Optional<Task> findByIdAndStageId(@Param("id") Long id, @Param("stageId") Long stageId);

    @Query("SELECT COUNT(t) FROM Task t WHERE t.assigneeUser.id = :userId " +
            "AND t.stage.constructionProject.id = :projectId AND t.status <> 'DONE'")
    long countActiveTasksForUserInProject(@Param("projectId") Long projectId, @Param("userId") Long userId);

    boolean existsByAssigneeUserId(Long userId);
}

