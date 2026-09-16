package br.pucpr.prissma_server.schedule;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ScheduleAllocationRepository extends JpaRepository<ScheduleAllocation, Long> {

    @Query("SELECT a FROM ScheduleAllocation a WHERE a.constructionProject.id = :projectId " +
            "AND a.allocationDate BETWEEN :startDate AND :endDate")
    List<ScheduleAllocation> findByProjectInPeriod(@Param("projectId") Long projectId,
                                                   @Param("startDate") LocalDate startDate,
                                                   @Param("endDate") LocalDate endDate);

    Optional<ScheduleAllocation> findByConstructionProjectIdAndUserIdAndAllocationDate(Long constructionProjectId,
                                                                                         Long userId,
                                                                                         LocalDate allocationDate);
}
