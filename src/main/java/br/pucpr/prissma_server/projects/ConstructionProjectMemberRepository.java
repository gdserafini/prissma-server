package br.pucpr.prissma_server.projects;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ConstructionProjectMemberRepository extends JpaRepository<ConstructionProjectMember, Long> {
    @Query("SELECT m.constructionProject FROM ConstructionProjectMember m WHERE m.user.id = :userId")
    List<ConstructionProject> findAllProjectsByUserId(@Param("userId") Long userId);

    @Query("SELECT m FROM ConstructionProjectMember m JOIN FETCH m.user JOIN FETCH m.constructionProject " +
            "WHERE m.constructionProject.id = :projectId ORDER BY m.joinedAt ASC, m.id ASC")
    List<ConstructionProjectMember> findAllByConstructionProjectIdOrderByJoinedAtAscIdAsc(@Param("projectId") Long projectId);

    Optional<ConstructionProjectMember> findByConstructionProjectIdAndUserId(Long constructionProjectId, Long userId);
}

