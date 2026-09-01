package br.pucpr.prissma_server.projects;

import br.pucpr.prissma_server.task.TaskRepository;
import br.pucpr.prissma_server.users.User;
import br.pucpr.prissma_server.workspaces.WorkspaceContext;
import br.pucpr.prissma_server.users.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import org.springframework.transaction.annotation.Transactional;

@Service
public class ConstructionProjectService {

    private final ConstructionProjectRepository repository;
    private final ConstructionProjectMemberRepository memberRepository;
    private final UserRepository userRepository;
    private final ProjectPermissionService permissionService;
    private final TaskRepository taskRepository;

    public ConstructionProjectService(ConstructionProjectRepository repository,
                                      ConstructionProjectMemberRepository memberRepository,
                                      UserRepository userRepository,
                                      ProjectPermissionService permissionService,
                                      TaskRepository taskRepository) {
        this.repository = repository;
        this.memberRepository = memberRepository;
        this.userRepository = userRepository;
        this.permissionService = permissionService;
        this.taskRepository = taskRepository;
    }

    private void requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
        }
    }

    @Transactional
    public ConstructionProject createProject(ConstructionProject project, Long ownerUserId) {
        requireText(project.getTitle(), "Title is required");
        requireText(project.getStreet(), "Street is required");
        requireText(project.getProjectType(), "Project type is required");
        requireText(project.getCategory(), "Category is required");
        if (project.getLandArea() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Land area is required");
        }
        if (project.getBuiltArea() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Built area is required");
        }
        // Criar obra exige workspace ativo e papel elevado NA CONTA (OWNER/ADMIN).
        // MEMBER/CLIENT agem via papéis de obra, nunca criam obras.
        WorkspaceContext ctx = WorkspaceContext.current();
        if (ctx == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Workspace not found");
        }
        if (!ctx.isElevated()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Only workspace OWNER or ADMIN can create projects");
        }
        if (repository.existsByWorkspaceIdAndTitle(ctx.workspaceId(), project.getTitle())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Project with this title already exists");
        }
        project.setWorkspaceId(ctx.workspaceId());
        Instant now = Instant.now();
        project.setCreatedAt(now);
        project.setUpdatedAt(now);
        ConstructionProject saved = repository.save(project);

        User owner = userRepository.findById(ownerUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        ConstructionProjectMember member = new ConstructionProjectMember();
        member.setConstructionProject(saved);
        member.setUser(owner);
        member.setRoleInProject("OWNER");
        member.setMembershipStatus("ACTIVE");
        member.setJoinedAt(now);
        memberRepository.save(member);

        return saved;
    }

    @Transactional
    public ConstructionProjectMember addMember(Long projectId, Long actorUserId, AddProjectMemberRequest request) {
        if (request == null || request.userId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User id is required");
        }

        ConstructionProject project = repository.findById(projectId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found"));

        permissionService.requirePermission(projectId, actorUserId, ProjectPermission.MANAGE_MEMBERS);

        User targetUser = userRepository.findById(request.userId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        if (request.roleInProject() == null || request.roleInProject().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Role in project is required");
        }

        String roleInProject = request.roleInProject().trim().toUpperCase();
        if (!List.of("ENGINEER", "ARCHITECT", "FOREMAN", "USER").contains(roleInProject)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Role in project must be ENGINEER, ARCHITECT, FOREMAN, or USER");
        }

        if (memberRepository.findByConstructionProjectIdAndUserId(projectId, targetUser.getId()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "User is already a member of this project");
        }

        Instant now = Instant.now();
        ConstructionProjectMember member = new ConstructionProjectMember();
        member.setConstructionProject(project);
        member.setUser(targetUser);
        member.setRoleInProject(roleInProject);
        member.setMembershipStatus("ACTIVE");
        member.setJoinedAt(now);

        return memberRepository.save(member);
    }

    @Transactional(readOnly = true)
    public List<ConstructionProjectMemberResponse> getMembers(Long projectId, Long userId) {
        repository.findById(projectId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found"));

        permissionService.requirePermission(projectId, userId, ProjectPermission.VIEW_PROJECT);

        return memberRepository.findAllByConstructionProjectIdOrderByJoinedAtAscIdAsc(projectId).stream()
                .map(ConstructionProjectMemberResponse::from)
                .toList();
    }

    @Transactional
    public ConstructionProjectMember updateMemberRole(Long projectId, Long actorUserId,
                                                      Long memberId, String roleInProject) {
        repository.findById(projectId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found"));

        permissionService.requirePermission(projectId, actorUserId, ProjectPermission.MANAGE_MEMBERS);

        ProjectRole newRole = ProjectRole.fromString(roleInProject);

        ConstructionProjectMember member = memberRepository.findById(memberId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Member not found"));

        if (!member.getConstructionProject().getId().equals(projectId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "User is not a member of this project");
        }

        member.setRoleInProject(newRole.name());
        return memberRepository.save(member);
    }

    @Transactional
    public void removeMember(Long projectId, Long actorUserId, Long memberId) {
        repository.findById(projectId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found"));

        permissionService.requirePermission(projectId, actorUserId, ProjectPermission.MANAGE_MEMBERS);

        ConstructionProjectMember member = memberRepository.findById(memberId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Member not found"));

        if (!member.getConstructionProject().getId().equals(projectId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "User is not a member of this project");
        }

        if (ProjectRole.OWNER.name().equals(member.getRoleInProject())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Cannot remove the project owner");
        }

        long activeTasks = taskRepository.countActiveTasksForUserInProject(projectId, member.getUser().getId());
        if (activeTasks > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Member has active tasks and cannot be removed");
        }

        memberRepository.delete(member);
    }

    @Transactional(readOnly = true)
    public RolePermissionsResponse getRolePermissions(Long projectId, Long actorUserId, String roleName) {
        repository.findById(projectId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found"));

        permissionService.requirePermission(projectId, actorUserId, ProjectPermission.VIEW_PROJECT);

        ProjectRole role = ProjectRole.fromString(roleName);
        Set<ProjectPermission> permissions = permissionService.effectivePermissions(projectId, role);
        return toRolePermissionsResponse(role, permissions);
    }

    @Transactional
    public RolePermissionsResponse updateRolePermissions(Long projectId, Long actorUserId,
                                                         String roleName, List<String> permissionNames) {
        ProjectRole role = ProjectRole.fromString(roleName);

        Set<ProjectPermission> requested = (permissionNames == null ? List.<String>of() : permissionNames)
                .stream()
                .map(ProjectPermission::fromString)
                .collect(java.util.stream.Collectors.toCollection(
                        () -> java.util.EnumSet.noneOf(ProjectPermission.class)));

        Set<ProjectPermission> result = permissionService.updateRolePermissions(projectId, actorUserId, role, requested);
        return toRolePermissionsResponse(role, result);
    }

    private RolePermissionsResponse toRolePermissionsResponse(ProjectRole role, Set<ProjectPermission> permissions) {
        List<String> names = permissions.stream().map(Enum::name).sorted().toList();
        return new RolePermissionsResponse(role.name(), names);
    }

    /** Todas as obras do sistema — só para staff da plataforma (Role.ADMIN, D7). */
    public List<ConstructionProject> getAll() {
        return repository.findAll();
    }

    /** Todas as obras do workspace ativo — visão de OWNER/ADMIN da conta. */
    public List<ConstructionProject> getAllForWorkspace(Long workspaceId) {
        return repository.findAllByWorkspaceIdOrderByIdAsc(workspaceId);
    }

    /** Obras onde o usuário é membro, dentro do workspace ativo — MEMBER/CLIENT. */
    public List<ConstructionProject> getAllForUser(Long userId, Long workspaceId) {
        return memberRepository.findAllProjectsByUserIdAndWorkspaceId(userId, workspaceId);
    }

    /**
     * Carrega a obra sem checar acesso. Uso interno: quem chama ja autorizou.
     * Toda entrada vinda de controller passa por {@link #getById(Long, Long)}.
     */
    private ConstructionProject loadById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found"));
    }

    public ConstructionProject getById(Long id, Long userId) {
        ConstructionProject project = loadById(id);
        permissionService.requirePermission(id, userId, ProjectPermission.VIEW_PROJECT);
        return project;
    }

    public ConstructionProject updateProject(Long id, ConstructionProjectRequest request, Long userId) {
        ConstructionProject project = loadById(id);
        permissionService.requirePermission(id, userId, ProjectPermission.MANAGE_PROJECT);

        if (request.title() != null && !request.title().isBlank() && !request.title().equals(project.getTitle())) {
            if (repository.existsByWorkspaceIdAndTitle(project.getWorkspaceId(), request.title())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Project with this title already exists");
            }
            project.setTitle(request.title());
        }
        if (request.cep() != null) project.setCep(request.cep());
        if (request.street() != null) project.setStreet(request.street());
        if (request.city() != null) project.setCity(request.city());
        if (request.state() != null) project.setState(request.state());
        if (request.number() != null) project.setNumber(request.number());
        if (request.complement() != null) project.setComplement(request.complement());
        if (request.projectType() != null) project.setProjectType(request.projectType());
        if (request.category() != null) project.setCategory(request.category());
        if (request.landArea() != null) project.setLandArea(request.landArea());
        if (request.builtArea() != null) project.setBuiltArea(request.builtArea());
        if (request.status() != null) project.setStatus(request.status());
        if (request.plannedStartDate() != null) project.setPlannedStartDate(request.plannedStartDate());
        if (request.plannedEndDate() != null) project.setPlannedEndDate(request.plannedEndDate());

        project.setUpdatedAt(Instant.now());
        return repository.save(project);
    }

    public void deleteProject(Long id, Long userId) {
        if (!repository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found");
        }
        permissionService.requirePermission(id, userId, ProjectPermission.MANAGE_PROJECT);
        repository.deleteById(id);
    }
}

