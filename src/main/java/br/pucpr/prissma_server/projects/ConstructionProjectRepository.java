package br.pucpr.prissma_server.projects;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ConstructionProjectRepository extends JpaRepository<ConstructionProject, Long> {
    // Unicidade de título é POR WORKSPACE (V14) — a checagem global foi removida
    // do repositório de propósito, para o compilador varrer qualquer uso restante.
    boolean existsByWorkspaceIdAndTitle(Long workspaceId, String title);

    List<ConstructionProject> findAllByWorkspaceIdOrderByIdAsc(Long workspaceId);
}

