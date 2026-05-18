package br.pucpr.prissma_server.stage;

import br.pucpr.prissma_server.projects.ConstructionProject;
import br.pucpr.prissma_server.projects.ConstructionProjectMember;
import br.pucpr.prissma_server.projects.ConstructionProjectMemberRepository;
import br.pucpr.prissma_server.projects.ConstructionProjectRepository;
import br.pucpr.prissma_server.users.Role;
import br.pucpr.prissma_server.users.User;
import br.pucpr.prissma_server.users.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("StageService Tests")
public class StageServiceTest {

    @Mock
    private StageRepository stageRepository;

    @Mock
    private ConstructionProjectRepository projectRepository;

    @Mock
    private ConstructionProjectMemberRepository memberRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private StageService service;

    private User ownerUser;
    private User engineerUser;
    private User foremanUser;
    private ConstructionProject project;
    private ConstructionProjectMember ownerMember;
    private ConstructionProjectMember engineerMember;
    private ConstructionProjectMember foremanMember;
    private StageRequest validRequest;

    @BeforeEach
    void setUp() {
        // Setup users
        ownerUser = new User();
        ownerUser.setId(1L);
        ownerUser.setEmail("owner@example.com");
        ownerUser.setRole(Role.USER);

        engineerUser = new User();
        engineerUser.setId(2L);
        engineerUser.setEmail("engineer@example.com");
        engineerUser.setRole(Role.USER);

        foremanUser = new User();
        foremanUser.setId(3L);
        foremanUser.setEmail("foreman@example.com");
        foremanUser.setRole(Role.USER);

        // Setup project
        project = new ConstructionProject();
        project.setId(1L);
        project.setTitle("Test Project");

        // Setup project members
        ownerMember = new ConstructionProjectMember();
        ownerMember.setConstructionProject(project);
        ownerMember.setUser(ownerUser);
        ownerMember.setRoleInProject("OWNER");

        engineerMember = new ConstructionProjectMember();
        engineerMember.setConstructionProject(project);
        engineerMember.setUser(engineerUser);
        engineerMember.setRoleInProject("ENGINEER");

        foremanMember = new ConstructionProjectMember();
        foremanMember.setConstructionProject(project);
        foremanMember.setUser(foremanUser);
        foremanMember.setRoleInProject("FOREMAN");

        // Setup valid request
        validRequest = new StageRequest(
                "Foundation",
                "Foundation work",
                1,
                "PLANNED",
                LocalDate.of(2026, 6, 1),
                LocalDate.of(2026, 7, 1),
                null,
                null
        );
    }

    // ============= CREATE TESTS =============
    @Test
    @DisplayName("Should create stage successfully with OWNER role")
    void testCreateStageSuccessWithOwner() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(ownerUser));
        when(memberRepository.findAll()).thenReturn(List.of(ownerMember));
        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));
        when(stageRepository.findByConstructionProjectIdAndDisplayOrder(1L, 1)).thenReturn(Optional.empty());

        Stage mockStage = new Stage();
        mockStage.setId(1L);
        mockStage.setConstructionProject(project);
        mockStage.setName("Foundation");
        mockStage.setDisplayOrder(1);
        mockStage.setStatus("PLANNED");
        mockStage.setCreatedAt(Instant.now());
        mockStage.setUpdatedAt(Instant.now());
        when(stageRepository.save(any(Stage.class))).thenReturn(mockStage);

        StageResponse response = service.create(1L, validRequest, 1L);

        assertNotNull(response);
        assertEquals("Foundation", response.getName());
        assertEquals(1, response.getDisplayOrder());
        verify(stageRepository, times(1)).save(any(Stage.class));
    }

    @Test
    @DisplayName("Should create stage successfully with ENGINEER role")
    void testCreateStageSuccessWithEngineer() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(engineerUser));
        when(memberRepository.findAll()).thenReturn(List.of(engineerMember));
        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));
        when(stageRepository.findByConstructionProjectIdAndDisplayOrder(1L, 1)).thenReturn(Optional.empty());

        Stage mockStage = new Stage();
        mockStage.setId(1L);
        mockStage.setConstructionProject(project);
        mockStage.setName("Foundation");
        mockStage.setDisplayOrder(1);
        mockStage.setCreatedAt(Instant.now());
        mockStage.setUpdatedAt(Instant.now());
        when(stageRepository.save(any(Stage.class))).thenReturn(mockStage);

        StageResponse response = service.create(1L, validRequest, 2L);

        assertNotNull(response);
        assertEquals("Foundation", response.getName());
        verify(stageRepository, times(1)).save(any(Stage.class));
    }

    @Test
    @DisplayName("Should reject creation with FOREMAN role")
    void testCreateStageUnauthorizedForeman() {
        when(userRepository.findById(3L)).thenReturn(Optional.of(foremanUser));
        when(memberRepository.findAll()).thenReturn(List.of(foremanMember));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.create(1L, validRequest, 3L));

        assertEquals("Only OWNER or ENGINEER can manage stages", exception.getReason());
    }

    @Test
    @DisplayName("Should reject creation when user not in project")
    void testCreateStageUserNotInProject() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(ownerUser));
        when(memberRepository.findAll()).thenReturn(new ArrayList<>());

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.create(1L, validRequest, 1L));

        assertEquals("User is not a member of this project", exception.getReason());
    }

    @Test
    @DisplayName("Should reject creation with invalid planned dates")
    void testCreateStageInvalidPlannedDates() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(ownerUser));
        when(memberRepository.findAll()).thenReturn(List.of(ownerMember));

        StageRequest invalidRequest = new StageRequest(
                "Foundation", "desc", 1, "PLANNED",
                LocalDate.of(2026, 7, 1),  // end
                LocalDate.of(2026, 6, 1),  // start
                null, null
        );

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.create(1L, invalidRequest, 1L));

        assertTrue(exception.getReason().contains("Planned start date cannot be after"));
    }

    @Test
    @DisplayName("Should reject creation with invalid actual dates")
    void testCreateStageInvalidActualDates() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(ownerUser));
        when(memberRepository.findAll()).thenReturn(List.of(ownerMember));

        StageRequest invalidRequest = new StageRequest(
                "Foundation", "desc", 1, "PLANNED",
                null, null,
                LocalDate.of(2026, 7, 1),  // end
                LocalDate.of(2026, 6, 1)  // start
        );

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.create(1L, invalidRequest, 1L));

        assertTrue(exception.getReason().contains("Actual start date cannot be after"));
    }

    @Test
    @DisplayName("Should reject creation with duplicate displayOrder")
    void testCreateStageDuplicateDisplayOrder() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(ownerUser));
        when(memberRepository.findAll()).thenReturn(List.of(ownerMember));
        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));

        Stage existingStage = new Stage();
        existingStage.setId(2L);
        when(stageRepository.findByConstructionProjectIdAndDisplayOrder(1L, 1))
                .thenReturn(Optional.of(existingStage));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.create(1L, validRequest, 1L));

        assertTrue(exception.getReason().contains("Stage with this display order already exists"));
    }

    @Test
    @DisplayName("Should reject creation when user not found")
    void testCreateStageUserNotFound() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.create(1L, validRequest, 99L));

        assertEquals("User not found", exception.getReason());
    }

    @Test
    @DisplayName("Should reject creation when project not found")
    void testCreateStageProjectNotFound() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(ownerUser));
        when(memberRepository.findAll()).thenReturn(List.of(ownerMember));
        when(projectRepository.findById(99L)).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.create(99L, validRequest, 1L));

        assertEquals("Project not found", exception.getReason());
    }

    // ============= LIST TESTS =============
    @Test
    @DisplayName("Should list stages by project in order")
    void testListByProjectSuccess() {
        Stage stage1 = new Stage();
        stage1.setId(1L);
        stage1.setName("Foundation");
        stage1.setDisplayOrder(1);
        stage1.setConstructionProject(project);
        stage1.setCreatedAt(Instant.now());
        stage1.setUpdatedAt(Instant.now());

        Stage stage2 = new Stage();
        stage2.setId(2L);
        stage2.setName("Structure");
        stage2.setDisplayOrder(2);
        stage2.setConstructionProject(project);
        stage2.setCreatedAt(Instant.now());
        stage2.setUpdatedAt(Instant.now());

        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));
        when(stageRepository.findByConstructionProjectIdOrderByDisplayOrder(1L))
                .thenReturn(List.of(stage1, stage2));

        List<StageResponse> responses = service.listByProject(1L);

        assertEquals(2, responses.size());
        assertEquals("Foundation", responses.get(0).getName());
        assertEquals("Structure", responses.get(1).getName());
    }

    @Test
    @DisplayName("Should return empty list when project has no stages")
    void testListByProjectEmpty() {
        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));
        when(stageRepository.findByConstructionProjectIdOrderByDisplayOrder(1L))
                .thenReturn(new ArrayList<>());

        List<StageResponse> responses = service.listByProject(1L);

        assertEquals(0, responses.size());
    }

    @Test
    @DisplayName("Should reject list when project not found")
    void testListByProjectNotFound() {
        when(projectRepository.findById(99L)).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.listByProject(99L));

        assertEquals("Project not found", exception.getReason());
    }

    // ============= GET TESTS =============
    @Test
    @DisplayName("Should get stage by id successfully")
    void testGetStageSuccess() {
        Stage stage = new Stage();
        stage.setId(1L);
        stage.setName("Foundation");
        stage.setDisplayOrder(1);
        stage.setConstructionProject(project);
        stage.setCreatedAt(Instant.now());
        stage.setUpdatedAt(Instant.now());

        when(stageRepository.findById(1L)).thenReturn(Optional.of(stage));

        StageResponse response = service.get(1L);

        assertNotNull(response);
        assertEquals("Foundation", response.getName());
        assertEquals(1L, response.getId());
    }

    @Test
    @DisplayName("Should reject get when stage not found")
    void testGetStageNotFound() {
        when(stageRepository.findById(99L)).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.get(99L));

        assertEquals("Stage not found", exception.getReason());
    }

    // ============= UPDATE TESTS =============
    @Test
    @DisplayName("Should update stage successfully")
    void testUpdateStageSuccess() {
        Stage existingStage = new Stage();
        existingStage.setId(1L);
        existingStage.setName("Foundation");
        existingStage.setDisplayOrder(1);
        existingStage.setStatus("PLANNED");
        existingStage.setConstructionProject(project);
        existingStage.setCreatedAt(Instant.now());
        existingStage.setUpdatedAt(Instant.now());

        StageRequest updateRequest = new StageRequest(
                "Updated Foundation", null, null, "IN_PROGRESS", null, null, null, null
        );

        when(stageRepository.findById(1L)).thenReturn(Optional.of(existingStage));
        when(userRepository.findById(1L)).thenReturn(Optional.of(ownerUser));
        when(memberRepository.findAll()).thenReturn(List.of(ownerMember));

        Stage updatedStage = new Stage();
        updatedStage.setId(1L);
        updatedStage.setName("Updated Foundation");
        updatedStage.setStatus("IN_PROGRESS");
        updatedStage.setConstructionProject(project);
        updatedStage.setCreatedAt(Instant.now());
        updatedStage.setUpdatedAt(Instant.now());

        when(stageRepository.save(any(Stage.class))).thenReturn(updatedStage);

        StageResponse response = service.update(1L, updateRequest, 1L);

        assertEquals("Updated Foundation", response.getName());
        assertEquals("IN_PROGRESS", response.getStatus());
    }

    @Test
    @DisplayName("Should update stage displayOrder successfully")
    void testUpdateStageDisplayOrder() {
        Stage existingStage = new Stage();
        existingStage.setId(1L);
        existingStage.setName("Foundation");
        existingStage.setDisplayOrder(1);
        existingStage.setConstructionProject(project);
        existingStage.setCreatedAt(Instant.now());
        existingStage.setUpdatedAt(Instant.now());

        StageRequest updateRequest = new StageRequest(
                null, null, 2, null, null, null, null, null
        );

        when(stageRepository.findById(1L)).thenReturn(Optional.of(existingStage));
        when(userRepository.findById(1L)).thenReturn(Optional.of(ownerUser));
        when(memberRepository.findAll()).thenReturn(List.of(ownerMember));
        when(stageRepository.findByConstructionProjectIdAndDisplayOrder(1L, 2)).thenReturn(Optional.empty());

        Stage updatedStage = new Stage();
        updatedStage.setId(1L);
        updatedStage.setDisplayOrder(2);
        updatedStage.setConstructionProject(project);
        updatedStage.setCreatedAt(Instant.now());
        updatedStage.setUpdatedAt(Instant.now());

        when(stageRepository.save(any(Stage.class))).thenReturn(updatedStage);

        StageResponse response = service.update(1L, updateRequest, 1L);

        assertEquals(2, response.getDisplayOrder());
    }

    @Test
    @DisplayName("Should reject update when stage not found")
    void testUpdateStageNotFound() {
        when(stageRepository.findById(99L)).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.update(99L, validRequest, 1L));

        assertEquals("Stage not found", exception.getReason());
    }

    @Test
    @DisplayName("Should reject update with duplicate displayOrder")
    void testUpdateStageDuplicateDisplayOrder() {
        Stage existingStage = new Stage();
        existingStage.setId(1L);
        existingStage.setDisplayOrder(1);
        existingStage.setConstructionProject(project);

        Stage conflictingStage = new Stage();
        conflictingStage.setId(2L);

        StageRequest updateRequest = new StageRequest(
                null, null, 2, null, null, null, null, null
        );

        when(stageRepository.findById(1L)).thenReturn(Optional.of(existingStage));
        when(userRepository.findById(1L)).thenReturn(Optional.of(ownerUser));
        when(memberRepository.findAll()).thenReturn(List.of(ownerMember));
        when(stageRepository.findByConstructionProjectIdAndDisplayOrder(1L, 2))
                .thenReturn(Optional.of(conflictingStage));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.update(1L, updateRequest, 1L));

        assertTrue(exception.getReason().contains("Stage with this display order already exists"));
    }

    // ============= DELETE TESTS =============
    @Test
    @DisplayName("Should delete stage successfully")
    void testDeleteStageSuccess() {
        Stage stage = new Stage();
        stage.setId(1L);
        stage.setConstructionProject(project);

        when(stageRepository.findById(1L)).thenReturn(Optional.of(stage));
        when(userRepository.findById(1L)).thenReturn(Optional.of(ownerUser));
        when(memberRepository.findAll()).thenReturn(List.of(ownerMember));

        service.delete(1L, 1L);

        verify(stageRepository, times(1)).deleteById(1L);
    }

    @Test
    @DisplayName("Should reject delete when stage not found")
    void testDeleteStageNotFound() {
        when(stageRepository.findById(99L)).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.delete(99L, 1L));

        assertEquals("Stage not found", exception.getReason());
    }

    @Test
    @DisplayName("Should reject delete when user not authorized")
    void testDeleteStageUnauthorized() {
        Stage stage = new Stage();
        stage.setId(1L);
        stage.setConstructionProject(project);

        when(stageRepository.findById(1L)).thenReturn(Optional.of(stage));
        when(userRepository.findById(3L)).thenReturn(Optional.of(foremanUser));
        when(memberRepository.findAll()).thenReturn(List.of(foremanMember));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.delete(1L, 3L));

        assertTrue(exception.getReason().contains("Only OWNER or ENGINEER"));
    }

    // ============= REORDER TESTS =============
    @Test
    @DisplayName("Should reorder stages successfully")
    void testReorderStagesSuccess() {
        Stage stage1 = new Stage();
        stage1.setId(1L);
        stage1.setDisplayOrder(1);
        stage1.setConstructionProject(project);

        Stage stage2 = new Stage();
        stage2.setId(2L);
        stage2.setDisplayOrder(2);
        stage2.setConstructionProject(project);

        List<Long> reorderedIds = List.of(2L, 1L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(ownerUser));
        when(memberRepository.findAll()).thenReturn(List.of(ownerMember));
        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));
        when(stageRepository.findById(2L)).thenReturn(Optional.of(stage2));
        when(stageRepository.findById(1L)).thenReturn(Optional.of(stage1));

        service.reorder(1L, reorderedIds, 1L);

        ArgumentCaptor<Stage> stageCaptor = ArgumentCaptor.forClass(Stage.class);
        verify(stageRepository, times(2)).save(stageCaptor.capture());

        List<Stage> savedStages = stageCaptor.getAllValues();
        assertEquals(1, savedStages.get(0).getDisplayOrder());
        assertEquals(2, savedStages.get(1).getDisplayOrder());
    }

    @Test
    @DisplayName("Should reject reorder when stage not in project")
    void testReorderStageNotInProject() {
        Stage stage1 = new Stage();
        stage1.setId(1L);
        ConstructionProject otherProject = new ConstructionProject();
        otherProject.setId(2L);
        stage1.setConstructionProject(otherProject);

        List<Long> reorderedIds = List.of(1L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(ownerUser));
        when(memberRepository.findAll()).thenReturn(List.of(ownerMember));
        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));
        when(stageRepository.findById(1L)).thenReturn(Optional.of(stage1));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.reorder(1L, reorderedIds, 1L));

        assertTrue(exception.getReason().contains("Stage does not belong to this project"));
    }

    @Test
    @DisplayName("Should reject reorder when stage not found")
    void testReorderStageNotFound() {
        List<Long> reorderedIds = List.of(99L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(ownerUser));
        when(memberRepository.findAll()).thenReturn(List.of(ownerMember));
        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));
        when(stageRepository.findById(99L)).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.reorder(1L, reorderedIds, 1L));

        assertTrue(exception.getReason().contains("Stage not found"));
    }

    // ============= MAPPER TESTS =============
    @Test
    @DisplayName("StageMapper should convert request to entity correctly")
    void testMapperRequestToEntity() {
        Stage stage = StageMapper.toEntity(validRequest, project);

        assertEquals("Foundation", stage.getName());
        assertEquals("Foundation work", stage.getDescription());
        assertEquals(1, stage.getDisplayOrder());
        assertEquals("PLANNED", stage.getStatus());
        assertEquals(project, stage.getConstructionProject());
    }

    @Test
    @DisplayName("StageMapper should convert entity to response correctly")
    void testMapperEntityToResponse() {
        Stage stage = new Stage();
        stage.setId(1L);
        stage.setName("Foundation");
        stage.setDisplayOrder(1);
        stage.setStatus("PLANNED");
        stage.setConstructionProject(project);
        stage.setCreatedAt(Instant.now());
        stage.setUpdatedAt(Instant.now());

        StageResponse response = StageMapper.toResponse(stage);

        assertEquals(1L, response.getId());
        assertEquals("Foundation", response.getName());
        assertEquals(1, response.getDisplayOrder());
        assertEquals(1L, response.getConstructionProjectId());
    }

    @Test
    @DisplayName("StageMapper should update entity with partial request")
    void testMapperUpdateEntity() {
        Stage stage = new Stage();
        stage.setName("Foundation");
        stage.setStatus("PLANNED");

        StageRequest updateRequest = new StageRequest(
                "New Name", null, null, "IN_PROGRESS", null, null, null, null
        );

        StageMapper.updateEntity(updateRequest, stage);

        assertEquals("New Name", stage.getName());
        assertEquals("IN_PROGRESS", stage.getStatus());
    }
}
