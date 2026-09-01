package br.pucpr.prissma_server.projects;

import br.pucpr.prissma_server.users.Role;
import br.pucpr.prissma_server.users.User;
import br.pucpr.prissma_server.users.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.time.Instant;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Set;

@Service
public class ProjectPermissionService {

    private final ConstructionProjectRepository projectRepository;
    private final ConstructionProjectMemberRepository memberRepository;
    private final ProjectRolePermissionRepository rolePermissionRepository;
    private final UserRepository userRepository;

    public ProjectPermissionService(ConstructionProjectRepository projectRepository,
                                    ConstructionProjectMemberRepository memberRepository,
                                    ProjectRolePermissionRepository rolePermissionRepository,
                                    UserRepository userRepository) {
        this.projectRepository = projectRepository;
        this.memberRepository = memberRepository;
        this.rolePermissionRepository = rolePermissionRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public Set<ProjectPermission> effectivePermissions(Long projectId, ProjectRole role) {
        var custom = rolePermissionRepository.findAllByConstructionProjectIdAndRole(projectId, role);
        if (custom.isEmpty()) {
            return role.getDefaultPermissions();
        }
        Set<ProjectPermission> permissions = EnumSet.noneOf(ProjectPermission.class);
        custom.forEach(rp -> permissions.add(rp.getPermission()));
        return permissions;
    }

    /**
     * Ponto único de decisão, agora com escopo de tenant:
     *   1. obra inexistente -> 404 (corrige o antigo 403 para id fantasma)
     *   2. Role.ADMIN global = staff da plataforma -> bypass (documentado, D7)
     *   3. obra fora do workspace ativo -> 404 GENÉRICO, nunca 403 — não
     *      revelar a existência de obras de outras construtoras
     *   4. OWNER/ADMIN do workspace -> todas as permissões da obra
     *   5. senão -> fluxo original: membership da obra + project_role_permissions
     */
    @Transactional(readOnly = true)
    public Set<ProjectPermission> permissionsForUser(Long projectId, Long userId) {
        ConstructionProject project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        if (user.getRole() == Role.ADMIN) {
            return EnumSet.allOf(ProjectPermission.class);
        }

        var ctx = br.pucpr.prissma_server.workspaces.WorkspaceContext.current();
        if (ctx == null || !project.getWorkspaceId().equals(ctx.workspaceId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found");
        }
        if (ctx.isElevated()) {
            return EnumSet.allOf(ProjectPermission.class);
        }

        ConstructionProjectMember member = memberRepository
                .findByConstructionProjectIdAndUserId(projectId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "User is not a member of this project"));

        if (!"ACTIVE".equals(member.getMembershipStatus())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "User is not an active member of this project");
        }

        return effectivePermissions(projectId, ProjectRole.fromString(member.getRoleInProject()));
    }

    @Transactional(readOnly = true)
    public void requirePermission(Long projectId, Long userId, ProjectPermission permission) {
        if (!permissionsForUser(projectId, userId).contains(permission)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "User does not have permission to perform this operation: " + permission);
        }
    }

    @Transactional
    public Set<ProjectPermission> updateRolePermissions(Long projectId, Long actorUserId,
                                                        ProjectRole role,
                                                        Collection<ProjectPermission> permissions) {
        ConstructionProject project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found"));

        requirePermission(projectId, actorUserId, ProjectPermission.MANAGE_MEMBERS);

        rolePermissionRepository.deleteAllByConstructionProjectIdAndRole(projectId, role);
        // Força a execução do DELETE antes dos INSERTs abaixo. Sem isso, o Hibernate
        // reordena as operações no flush (INSERTs antes de DELETEs) e as linhas antigas
        // ainda presentes violam a constraint única (obra, papel, permissão).
        rolePermissionRepository.flush();

        Set<ProjectPermission> result = permissions.isEmpty()
                ? EnumSet.noneOf(ProjectPermission.class)
                : EnumSet.copyOf(permissions);

        Instant now = Instant.now();
        for (ProjectPermission permission : result) {
            ProjectRolePermission entity = new ProjectRolePermission();
            entity.setConstructionProject(project);
            entity.setRole(role);
            entity.setPermission(permission);
            entity.setCreatedAt(now);
            rolePermissionRepository.save(entity);
        }

        return result;
    }
}
