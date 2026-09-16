package br.pucpr.prissma_server.schedule;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ScheduleMemberRepository extends JpaRepository<ScheduleMember, Long> {

    List<ScheduleMember> findAllByConstructionProjectId(Long constructionProjectId);

    Optional<ScheduleMember> findByConstructionProjectIdAndUserId(Long constructionProjectId, Long userId);
}
