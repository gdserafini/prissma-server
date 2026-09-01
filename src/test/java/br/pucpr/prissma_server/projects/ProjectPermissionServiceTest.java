package br.pucpr.prissma_server.projects;

import br.pucpr.prissma_server.users.Role;
import br.pucpr.prissma_server.users.User;
import br.pucpr.prissma_server.users.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
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
 * Primeiro teste do choke point de autorização. Toda a lógica de decisão
 * (bypass de ADMIN, membership, overrides por obra) vive aqui — e é aqui
 * que o escopo de workspace entra na Fase 2. Cada regra abaixo é uma
 * invariante que a introdução do workspace_id não pode quebrar.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ProjectPermissionService Tests")
class ProjectPermissionServiceTest {

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
        project.setId(100L);
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
    @DisplayName("Staff ADMIN bypasses membership and gets every permission")
    void adminBypass() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(staffAdmin));

        Set<ProjectPermission> permissions = service.permissionsForUser(100L, 1L);

        assertEquals(EnumSet.allOf(ProjectPermission.class), permissions);
    }

    @Test
    @DisplayName("Unknown user -> 404")
    void userNotFound() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.permissionsForUser(100L, 99L));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    @DisplayName("Non-member -> 403")
    void nonMemberForbidden() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(regularUser));
        when(memberRepository.findByConstructionProjectIdAndUserId(100L, 2L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.permissionsForUser(100L, 2L));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    @DisplayName("Inactive member -> 403")
    void inactiveMemberForbidden() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(regularUser));
        when(memberRepository.findByConstructionProjectIdAndUserId(100L, 2L))
                .thenReturn(Optional.of(member("ENGINEER", "INACTIVE")));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.permissionsForUser(100L, 2L));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    @DisplayName("Active member without overrides gets the role defaults")
    void activeMemberGetsRoleDefaults() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(regularUser));
        when(memberRepository.findByConstructionProjectIdAndUserId(100L, 2L))
                .thenReturn(Optional.of(member("FOREMAN", "ACTIVE")));
        when(rolePermissionRepository.findAllByConstructionProjectIdAndRole(100L, ProjectRole.FOREMAN))
                .thenReturn(List.of());

        Set<ProjectPermission> permissions = service.permissionsForUser(100L, 2L);

        assertEquals(ProjectRole.FOREMAN.getDefaultPermissions(), permissions);
    }

    @Test
    @DisplayName("Custom rows REPLACE the defaults entirely (no merge)")
    void customPermissionsReplaceDefaults() {
        ProjectRolePermission custom = new ProjectRolePermission();
        custom.setConstructionProject(project);
        custom.setRole(ProjectRole.FOREMAN);
        custom.setPermission(ProjectPermission.VIEW_PROJECT);

        when(userRepository.findById(2L)).thenReturn(Optional.of(regularUser));
        when(memberRepository.findByConstructionProjectIdAndUserId(100L, 2L))
                .thenReturn(Optional.of(member("FOREMAN", "ACTIVE")));
        when(rolePermissionRepository.findAllByConstructionProjectIdAndRole(100L, ProjectRole.FOREMAN))
                .thenReturn(List.of(custom));

        Set<ProjectPermission> permissions = service.permissionsForUser(100L, 2L);

        assertEquals(EnumSet.of(ProjectPermission.VIEW_PROJECT), permissions);
    }

    @Test
    @DisplayName("requirePermission passes silently when the permission is held")
    void requirePermissionGranted() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(regularUser));
        when(memberRepository.findByConstructionProjectIdAndUserId(100L, 2L))
                .thenReturn(Optional.of(member("USER", "ACTIVE")));
        when(rolePermissionRepository.findAllByConstructionProjectIdAndRole(100L, ProjectRole.USER))
                .thenReturn(List.of());

        assertDoesNotThrow(() -> service.requirePermission(100L, 2L, ProjectPermission.VIEW_PROJECT));
    }

    @Test
    @DisplayName("requirePermission -> 403 when the permission is missing")
    void requirePermissionDenied() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(regularUser));
        when(memberRepository.findByConstructionProjectIdAndUserId(100L, 2L))
                .thenReturn(Optional.of(member("USER", "ACTIVE")));
        when(rolePermissionRepository.findAllByConstructionProjectIdAndRole(100L, ProjectRole.USER))
                .thenReturn(List.of());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.requirePermission(100L, 2L, ProjectPermission.MANAGE_STAGES));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    @DisplayName("updateRolePermissions deletes, flushes, then inserts (unique constraint ordering)")
    void updateRolePermissionsDeletesThenFlushesThenInserts() {
        when(projectRepository.findById(100L)).thenReturn(Optional.of(project));
        when(userRepository.findById(1L)).thenReturn(Optional.of(staffAdmin)); // ADMIN bypass no requirePermission interno

        Set<ProjectPermission> result = service.updateRolePermissions(100L, 1L, ProjectRole.FOREMAN,
                EnumSet.of(ProjectPermission.VIEW_PROJECT, ProjectPermission.MANAGE_TASKS));

        assertEquals(EnumSet.of(ProjectPermission.VIEW_PROJECT, ProjectPermission.MANAGE_TASKS), result);

        InOrder order = inOrder(rolePermissionRepository);
        order.verify(rolePermissionRepository).deleteAllByConstructionProjectIdAndRole(100L, ProjectRole.FOREMAN);
        order.verify(rolePermissionRepository).flush();
        order.verify(rolePermissionRepository, times(2)).save(any(ProjectRolePermission.class));
    }

    @Test
    @DisplayName("updateRolePermissions with an empty list persists nothing")
    void updateRolePermissionsEmptyListPersistsNothing() {
        when(projectRepository.findById(100L)).thenReturn(Optional.of(project));
        when(userRepository.findById(1L)).thenReturn(Optional.of(staffAdmin));

        Set<ProjectPermission> result = service.updateRolePermissions(100L, 1L, ProjectRole.FOREMAN, List.of());

        assertTrue(result.isEmpty());
        verify(rolePermissionRepository, times(0)).save(any(ProjectRolePermission.class));
    }
}
