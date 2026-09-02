package br.pucpr.prissma_server.projects;

import br.pucpr.prissma_server.users.Role;
import br.pucpr.prissma_server.users.User;
import br.pucpr.prissma_server.users.UserRepository;
import br.pucpr.prissma_server.workspaces.WorkspaceContext;
import br.pucpr.prissma_server.workspaces.WorkspaceRole;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;

import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Teste do choke point de autorização, agora com escopo de tenant:
 *
 *   obra inexistente -> 404 · staff ADMIN -> bypass · obra fora do workspace
 *   ativo (ou sem contexto) -> 404 genérico · OWNER/ADMIN do workspace ->
 *   todas as permissões · senão membership da obra + project_role_permissions.
 *
 * Cada regra abaixo é uma invariante do isolamento entre construtoras.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ProjectPermissionService Tests")
class ProjectPermissionServiceTest {

    private static final Long PROJECT_ID = 100L;
    private static final Long WORKSPACE_ID = 7L;

    @Mock
    private ConstructionProjectRepository projectRepository;

    @Mock
    private ConstructionProjectMemberRepository memberRepository;

    @Mock
    private ProjectRolePermissionRepository rolePermissionRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ProjectPermissionService service;

    private User staffAdmin;
    private User regularUser;
    private ConstructionProject project;

    @BeforeEach
    void setUp() {
        staffAdmin = new User();
        staffAdmin.setId(1L);
        staffAdmin.setRole(Role.ADMIN);

        regularUser = new User();
        regularUser.setId(2L);
        regularUser.setRole(Role.USER);

        project = new ConstructionProject();
        project.setId(PROJECT_ID);
        project.setWorkspaceId(WORKSPACE_ID);
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    private void authenticate(Long userId, WorkspaceContext ctx) {
        var auth = new UsernamePasswordAuthenticationToken(userId, null, List.of());
        auth.setDetails(ctx);
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    private void stubProject() {
        when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.of(project));
    }

    private WorkspaceContext memberCtx() {
        return new WorkspaceContext(WORKSPACE_ID, WorkspaceRole.MEMBER, false);
    }

    private ConstructionProjectMember member(String roleInProject, String status) {
        ConstructionProjectMember m = new ConstructionProjectMember();
        m.setConstructionProject(project);
        m.setUser(regularUser);
        m.setRoleInProject(roleInProject);
        m.setMembershipStatus(status);
        return m;
    }

    @Test
    @DisplayName("Unknown project -> 404 (never 403 for a phantom id)")
    void projectNotFound() {
        when(projectRepository.findById(999L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.permissionsForUser(999L, 2L));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    @DisplayName("Staff ADMIN bypasses workspace and membership entirely")
    void adminBypass() {
        stubProject();
        when(userRepository.findById(1L)).thenReturn(Optional.of(staffAdmin));

        Set<ProjectPermission> permissions = service.permissionsForUser(PROJECT_ID, 1L);

        assertEquals(EnumSet.allOf(ProjectPermission.class), permissions);
    }

    @Test
    @DisplayName("Unknown user -> 404")
    void userNotFound() {
        stubProject();
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.permissionsForUser(PROJECT_ID, 99L));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    @DisplayName("No workspace context -> 404 generic (anti-enumeration)")
    void noContextNotFound() {
        stubProject();
        when(userRepository.findById(2L)).thenReturn(Optional.of(regularUser));
        authenticate(2L, null);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.permissionsForUser(PROJECT_ID, 2L));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    @DisplayName("Project from another workspace -> 404 generic, never 403")
    void crossWorkspaceNotFound() {
        stubProject();
        when(userRepository.findById(2L)).thenReturn(Optional.of(regularUser));
        authenticate(2L, new WorkspaceContext(999L, WorkspaceRole.OWNER, true));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.permissionsForUser(PROJECT_ID, 2L));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    @DisplayName("Workspace OWNER/ADMIN gets every project permission without membership")
    void elevatedWorkspaceRoleBypassesMembership() {
        stubProject();
        when(userRepository.findById(2L)).thenReturn(Optional.of(regularUser));
        authenticate(2L, new WorkspaceContext(WORKSPACE_ID, WorkspaceRole.ADMIN, false));

        Set<ProjectPermission> permissions = service.permissionsForUser(PROJECT_ID, 2L);

        assertEquals(EnumSet.allOf(ProjectPermission.class), permissions);
    }

    @Test
    @DisplayName("Same-workspace non-member -> 403 (existence is not a secret inside the tenant)")
    void nonMemberForbidden() {
        stubProject();
        when(userRepository.findById(2L)).thenReturn(Optional.of(regularUser));
        when(memberRepository.findByConstructionProjectIdAndUserId(PROJECT_ID, 2L)).thenReturn(Optional.empty());
        authenticate(2L, memberCtx());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.permissionsForUser(PROJECT_ID, 2L));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    @DisplayName("Inactive member -> 403")
    void inactiveMemberForbidden() {
        stubProject();
        when(userRepository.findById(2L)).thenReturn(Optional.of(regularUser));
        when(memberRepository.findByConstructionProjectIdAndUserId(PROJECT_ID, 2L))
                .thenReturn(Optional.of(member("ENGINEER", "INACTIVE")));
        authenticate(2L, memberCtx());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.permissionsForUser(PROJECT_ID, 2L));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    @DisplayName("Active member without overrides gets the role defaults")
    void activeMemberGetsRoleDefaults() {
        stubProject();
        when(userRepository.findById(2L)).thenReturn(Optional.of(regularUser));
        when(memberRepository.findByConstructionProjectIdAndUserId(PROJECT_ID, 2L))
                .thenReturn(Optional.of(member("FOREMAN", "ACTIVE")));
        when(rolePermissionRepository.findAllByConstructionProjectIdAndRole(PROJECT_ID, ProjectRole.FOREMAN))
                .thenReturn(List.of());
        authenticate(2L, memberCtx());

        Set<ProjectPermission> permissions = service.permissionsForUser(PROJECT_ID, 2L);

        assertEquals(ProjectRole.FOREMAN.getDefaultPermissions(), permissions);
    }

    @Test
    @DisplayName("Custom rows REPLACE the defaults entirely (no merge)")
    void customPermissionsReplaceDefaults() {
        ProjectRolePermission custom = new ProjectRolePermission();
        custom.setConstructionProject(project);
        custom.setRole(ProjectRole.FOREMAN);
        custom.setPermission(ProjectPermission.VIEW_PROJECT);

        stubProject();
        when(userRepository.findById(2L)).thenReturn(Optional.of(regularUser));
        when(memberRepository.findByConstructionProjectIdAndUserId(PROJECT_ID, 2L))
                .thenReturn(Optional.of(member("FOREMAN", "ACTIVE")));
        when(rolePermissionRepository.findAllByConstructionProjectIdAndRole(PROJECT_ID, ProjectRole.FOREMAN))
                .thenReturn(List.of(custom));
        authenticate(2L, memberCtx());

        Set<ProjectPermission> permissions = service.permissionsForUser(PROJECT_ID, 2L);

        assertEquals(EnumSet.of(ProjectPermission.VIEW_PROJECT), permissions);
    }

    @Test
    @DisplayName("requirePermission passes silently when the permission is held")
    void requirePermissionGranted() {
        stubProject();
        when(userRepository.findById(2L)).thenReturn(Optional.of(regularUser));
        when(memberRepository.findByConstructionProjectIdAndUserId(PROJECT_ID, 2L))
                .thenReturn(Optional.of(member("USER", "ACTIVE")));
        when(rolePermissionRepository.findAllByConstructionProjectIdAndRole(PROJECT_ID, ProjectRole.USER))
                .thenReturn(List.of());
        authenticate(2L, memberCtx());

        assertDoesNotThrow(() -> service.requirePermission(PROJECT_ID, 2L, ProjectPermission.VIEW_PROJECT));
    }

    @Test
    @DisplayName("requirePermission -> 403 when the permission is missing")
    void requirePermissionDenied() {
        stubProject();
        when(userRepository.findById(2L)).thenReturn(Optional.of(regularUser));
        when(memberRepository.findByConstructionProjectIdAndUserId(PROJECT_ID, 2L))
                .thenReturn(Optional.of(member("USER", "ACTIVE")));
        when(rolePermissionRepository.findAllByConstructionProjectIdAndRole(PROJECT_ID, ProjectRole.USER))
                .thenReturn(List.of());
        authenticate(2L, memberCtx());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.requirePermission(PROJECT_ID, 2L, ProjectPermission.MANAGE_STAGES));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    @DisplayName("updateRolePermissions deletes, flushes, then inserts (unique constraint ordering)")
    void updateRolePermissionsDeletesThenFlushesThenInserts() {
        stubProject();
        when(userRepository.findById(1L)).thenReturn(Optional.of(staffAdmin)); // ADMIN bypass no requirePermission interno

        Set<ProjectPermission> result = service.updateRolePermissions(PROJECT_ID, 1L, ProjectRole.FOREMAN,
                EnumSet.of(ProjectPermission.VIEW_PROJECT, ProjectPermission.MANAGE_TASKS));

        assertEquals(EnumSet.of(ProjectPermission.VIEW_PROJECT, ProjectPermission.MANAGE_TASKS), result);

        InOrder order = inOrder(rolePermissionRepository);
        order.verify(rolePermissionRepository).deleteAllByConstructionProjectIdAndRole(PROJECT_ID, ProjectRole.FOREMAN);
        order.verify(rolePermissionRepository).flush();
        order.verify(rolePermissionRepository, times(2)).save(any(ProjectRolePermission.class));
    }

    @Test
    @DisplayName("updateRolePermissions with an empty list persists nothing")
    void updateRolePermissionsEmptyListPersistsNothing() {
        stubProject();
        when(userRepository.findById(1L)).thenReturn(Optional.of(staffAdmin));

        Set<ProjectPermission> result = service.updateRolePermissions(PROJECT_ID, 1L, ProjectRole.FOREMAN, List.of());

        assertTrue(result.isEmpty());
        verify(rolePermissionRepository, times(0)).save(any(ProjectRolePermission.class));
    }
}
