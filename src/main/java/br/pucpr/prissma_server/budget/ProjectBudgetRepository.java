package br.pucpr.prissma_server.budget;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProjectBudgetRepository extends JpaRepository<ProjectBudget, Long> {

    Optional<ProjectBudget> findByConstructionProjectId(Long constructionProjectId);

    boolean existsByConstructionProjectId(Long constructionProjectId);
}
