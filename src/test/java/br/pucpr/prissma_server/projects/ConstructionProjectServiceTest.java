package br.pucpr.prissma_server.projects;

import br.pucpr.prissma_server.task.TaskRepository;
import br.pucpr.prissma_server.users.Role;
import br.pucpr.prissma_server.users.User;
import br.pucpr.prissma_server.users.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ConstructionProjectService Tests")
class ConstructionProjectServiceTest {

    @Mock
    private ConstructionProjectRepository repository;

    @Mock
    private ConstructionProjectMemberRepository memberRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ProjectPermissionService permissionService;

    @Mock
    private TaskRepository taskRepository;

    @InjectMocks
    private ConstructionProjectService service;

    private ConstructionProject project;
    private User ownerUser;
    private User engineerUser;
    private User targetUser;
    private ConstructionProjectMember ownerMember;
    private User adminUser;
    private ConstructionProjectMember engineerMember;

    private ConstructionProjectMember targetMember;
    @BeforeEach
    void setUp() {
        project = new ConstructionProject();
        project.setId(1L);
        project.setTitle("Obra de Teste");
        project.setStreet("Rua das Flores");
        project.setProjectType("RESIDENTIAL");
        project.setCategory("HOUSE");
        project.setLandArea(new BigDecimal("250.00"));
        project.setBuiltArea(new BigDecimal("180.00"));
        project.setCreatedAt(Instant.now());
        project.setUpdatedAt(Instant.now());

        ownerUser = new User();
        ownerUser.setId(10L);
        ownerUser.setName("Owner");
        ownerUser.setEmail("owner@example.com");
        ownerUser.setRole(Role.USER);

        engineerUser = new User();
        engineerUser.setId(11L);
        engineerUser.setName("Engineer");
        engineerUser.setEmail("engineer@example.com");
        engineerUser.setRole(Role.USER);

        targetUser = new User();
        targetUser.setId(20L);
        targetUser.setName("Target");
        targetUser.setEmail("target@example.com");
        targetUser.setRole(Role.USER);

        ownerMember = new ConstructionProjectMember();
        adminUser = new User();
        adminUser.setId(99L);
        adminUser.setName("Admin");
        adminUser.setEmail("admin@example.com");
        adminUser.setRole(Role.ADMIN);

        ownerMember.setConstructionProject(project);
        ownerMember.setUser(ownerUser);
        ownerMember.setRoleInProject("OWNER");
        ownerMember.setMembershipStatus("ACTIVE");
        ownerMember.setJoinedAt(Instant.now());

        engineerMember = new ConstructionProjectMember();
        engineerMember.setConstructionProject(project);
        engineerMember.setUser(engineerUser);
        engineerMember.setRoleInProject("ENGINEER");
        engineerMember.setMembershipStatus("ACTIVE");
        engineerMember.setJoinedAt(Instant.now());

        targetMember = new ConstructionProjectMember();
        targetMember.setConstructionProject(project);
        targetMember.setUser(targetUser);
        targetMember.setRoleInProject("FOREMAN");
        targetMember.setMembershipStatus("ACTIVE");
        targetMember.setJoinedAt(Instant.now());
    }

    @Test
    @DisplayName("OWNER can add a member to the project")
    void addMember_asOwner_succeeds() {
        when(repository.findById(1L)).thenReturn(Optional.of(project));
        when(userRepository.findById(20L)).thenReturn(Optional.of(targetUser));
        when(memberRepository.findByConstructionProjectIdAndUserId(1L, 20L)).thenReturn(Optional.empty());
        when(memberRepository.save(any(ConstructionProjectMember.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ConstructionProjectMember member = service.addMember(1L, 10L, new AddProjectMemberRequest(20L, "FOREMAN"));

        assertNotNull(member);
        assertEquals(1L, member.getConstructionProject().getId());
        assertEquals(20L, member.getUser().getId());
        assertEquals("FOREMAN", member.getRoleInProject());
        assertEquals("ACTIVE", member.getMembershipStatus());
    }

    @Test
    @DisplayName("ENGINEER can add a member to the project")
    void addMember_asEngineer_succeeds() {
        when(repository.findById(1L)).thenReturn(Optional.of(project));
        when(userRepository.findById(20L)).thenReturn(Optional.of(targetUser));
        when(memberRepository.findByConstructionProjectIdAndUserId(1L, 20L)).thenReturn(Optional.empty());
        when(memberRepository.save(any(ConstructionProjectMember.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ConstructionProjectMember member = service.addMember(1L, 11L, new AddProjectMemberRequest(20L, "ARCHITECT"));

        assertNotNull(member);
        assertEquals("ARCHITECT", member.getRoleInProject());
    }

    @Test
    @DisplayName("FOREMAN cannot add members")
    void addMember_asForeman_forbidden() {
        User foremanUser = new User();
        foremanUser.setId(12L);
        foremanUser.setName("Foreman");
        foremanUser.setEmail("foreman@example.com");
        foremanUser.setRole(Role.USER);

        ConstructionProjectMember foremanMember = new ConstructionProjectMember();
        foremanMember.setConstructionProject(project);
        foremanMember.setUser(foremanUser);
        foremanMember.setRoleInProject("FOREMAN");
        foremanMember.setMembershipStatus("ACTIVE");
        foremanMember.setJoinedAt(Instant.now());

        when(repository.findById(1L)).thenReturn(Optional.of(project));
        // A decisão de permissão vive no ProjectPermissionService (coberto em
        // ProjectPermissionServiceTest); aqui testamos só o repasse da negação.
        doThrow(new ResponseStatusException(HttpStatus.FORBIDDEN,
                "User does not have permission to perform this operation: MANAGE_MEMBERS"))
                .when(permissionService).requirePermission(1L, 12L, ProjectPermission.MANAGE_MEMBERS);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.addMember(1L, 12L, new AddProjectMemberRequest(20L, "FOREMAN")));

        assertEquals("User does not have permission to perform this operation: MANAGE_MEMBERS",
                exception.getReason());
    }

    @Test
    @DisplayName("Adding an existing member returns conflict")
    void addMember_duplicate_conflict() {
        when(repository.findById(1L)).thenReturn(Optional.of(project));
        when(userRepository.findById(20L)).thenReturn(Optional.of(targetUser));
        when(memberRepository.findByConstructionProjectIdAndUserId(1L, 20L)).thenReturn(Optional.of(new ConstructionProjectMember()));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.addMember(1L, 10L, new AddProjectMemberRequest(20L, "FOREMAN")));

        assertEquals("User is already a member of this project", exception.getReason());
    }

    @Test
    @DisplayName("Unknown target user returns not found")
    void addMember_targetUserNotFound() {
        when(repository.findById(1L)).thenReturn(Optional.of(project));
        when(userRepository.findById(20L)).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.addMember(1L, 10L, new AddProjectMemberRequest(20L, "FOREMAN")));

        assertEquals("User not found", exception.getReason());
    }

    @Test
    @DisplayName("Invalid role in project is rejected")
    void addMember_invalidRoleRejected() {
        when(repository.findById(1L)).thenReturn(Optional.of(project));
        when(userRepository.findById(20L)).thenReturn(Optional.of(targetUser));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.addMember(1L, 10L, new AddProjectMemberRequest(20L, "OWNER")));

        assertEquals("Role in project must be ENGINEER, ARCHITECT, FOREMAN, or USER", exception.getReason());
    }


    @Test
    @DisplayName("OWNER can list project members")
    void getMembers_asOwner_succeeds() {
        when(repository.findById(1L)).thenReturn(Optional.of(project));
        when(memberRepository.findAllByConstructionProjectIdOrderByJoinedAtAscIdAsc(1L))
                .thenReturn(List.of(ownerMember, engineerMember, targetMember));

        List<ConstructionProjectMemberResponse> members = service.getMembers(1L, 10L);

        assertEquals(3, members.size());
        assertEquals(10L, members.get(0).user().id());
        assertEquals(11L, members.get(1).user().id());
        assertEquals(20L, members.get(2).user().id());
    }

    @Test
    @DisplayName("ADMIN can list project members")
    void getMembers_asAdmin_succeeds() {
        when(repository.findById(1L)).thenReturn(Optional.of(project));
        when(memberRepository.findAllByConstructionProjectIdOrderByJoinedAtAscIdAsc(1L))
                .thenReturn(List.of(ownerMember, engineerMember));

        List<ConstructionProjectMemberResponse> members = service.getMembers(1L, 99L);

        assertEquals(2, members.size());
        assertEquals("OWNER", members.getFirst().roleInProject());
    }

    @Test
    @DisplayName("Non-member cannot list project members")
    void getMembers_forbiddenForNonMember() {
        User outsider = new User();
        outsider.setId(30L);
        outsider.setName("Outsider");
        outsider.setEmail("outsider@example.com");
        outsider.setRole(Role.USER);

        when(repository.findById(1L)).thenReturn(Optional.of(project));
        doThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "User is not a member of this project"))
                .when(permissionService).requirePermission(1L, 30L, ProjectPermission.VIEW_PROJECT);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.getMembers(1L, 30L));

        assertEquals("User is not a member of this project", exception.getReason());
    }

    @Test
    @DisplayName("Listing members for a missing project returns not found")
    void getMembers_projectNotFound() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.getMembers(99L, 10L));

        assertEquals("Project not found", exception.getReason());
    }
}
