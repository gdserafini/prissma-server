package br.pucpr.prissma_server.projects;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ConstructionProjectRepository extends JpaRepository<ConstructionProject, Long> {
    Optional<ConstructionProject> findByTitle(String title);
    boolean existsByTitle(String title);
}

