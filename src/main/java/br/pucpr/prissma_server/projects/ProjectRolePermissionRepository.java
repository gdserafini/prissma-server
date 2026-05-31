package br.pucpr.prissma_server.projects;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProjectRolePermissionRepository extends JpaRepository<ProjectRolePermission, Long> {

    List<ProjectRolePermission> findAllByConstructionProjectIdAndRole(Long constructionProjectId, ProjectRole role);

    void deleteAllByConstructionProjectIdAndRole(Long constructionProjectId, ProjectRole role);
}
