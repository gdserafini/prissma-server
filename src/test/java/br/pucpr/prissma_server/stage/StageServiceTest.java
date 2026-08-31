package br.pucpr.prissma_server.stage;

import br.pucpr.prissma_server.projects.ConstructionProject;
import br.pucpr.prissma_server.projects.ConstructionProjectRepository;
import br.pucpr.prissma_server.projects.ProjectPermission;
import br.pucpr.prissma_server.projects.ProjectPermissionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("StageService Tests")
public class StageServiceTest {

    @Mock
    private StageRepository stageRepository;

    @Mock
    private ConstructionProjectRepository projectRepository;

    @Mock
    private ProjectPermissionService permissionService;

    @InjectMocks
    private StageService service;

    private ConstructionProject project;
    private StageRequest validRequest;

    private void assertReasonContains(ResponseStatusException exception, String expectedPart) {
        assertNotNull(exception.getReason());
        assertTrue(exception.getReason().contains(expectedPart));
    }

    @BeforeEach
    void setUp() {
        project = new ConstructionProject();
        project.setId(1L);
        project.setTitle("Test Project");

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
        doThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "User does not have permission to perform this operation: MANAGE_STAGES"))
                .when(permissionService).requirePermission(1L, 3L, ProjectPermission.MANAGE_STAGES);


        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.create(1L, validRequest, 3L));

        assertEquals("User does not have permission to perform this operation: MANAGE_STAGES",
                exception.getReason());
    }

    @Test
    @DisplayName("Should reject creation when user not in project")
    void testCreateStageUserNotInProject() {
        doThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "User is not a member of this project"))
                .when(permissionService).requirePermission(1L, 1L, ProjectPermission.MANAGE_STAGES);


        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.create(1L, validRequest, 1L));

        assertEquals("User is not a member of this project", exception.getReason());
    }

    @Test
    @DisplayName("Should reject creation with invalid planned dates")
    void testCreateStageInvalidPlannedDates() {
        StageRequest invalidRequest = new StageRequest(
                "Foundation", "desc", 1, "PLANNED",
                LocalDate.of(2026, 7, 1),  // end
                LocalDate.of(2026, 6, 1),  // start
                null, null
        );

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.create(1L, invalidRequest, 1L));

        assertReasonContains(exception, "Planned start date cannot be after");
    }

    @Test
    @DisplayName("Should reject creation with invalid actual dates")
    void testCreateStageInvalidActualDates() {

        StageRequest invalidRequest = new StageRequest(
                "Foundation", "desc", 1, "PLANNED",
                null, null,
                LocalDate.of(2026, 7, 1),  // end
                LocalDate.of(2026, 6, 1)  // start
        );

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.create(1L, invalidRequest, 1L));

        assertReasonContains(exception, "Actual start date cannot be after");
    }

    @Test
    @DisplayName("Should reject creation with duplicate displayOrder")
    void testCreateStageDuplicateDisplayOrder() {
        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));

        Stage existingStage = new Stage();
        existingStage.setId(2L);
        when(stageRepository.findByConstructionProjectIdAndDisplayOrder(1L, 1))
                .thenReturn(Optional.of(existingStage));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.create(1L, validRequest, 1L));

        assertReasonContains(exception, "Stage with this display order already exists");
    }

    @Test
    @DisplayName("Should reject creation when user not found")
    void testCreateStageUserNotFound() {
        doThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"))
                .when(permissionService).requirePermission(1L, 99L, ProjectPermission.MANAGE_STAGES);


        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.create(1L, validRequest, 99L));

        assertEquals("User not found", exception.getReason());
    }

    @Test
    @DisplayName("Should reject creation when project not found")
    void testCreateStageProjectNotFound() {
        when(projectRepository.findById(99L)).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.create(99L, validRequest, 1L));

        assertNotNull(exception.getReason());
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

        List<StageResponse> responses = service.listByProject(1L, 1L);

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

        List<StageResponse> responses = service.listByProject(1L, 1L);

        assertEquals(0, responses.size());
    }

    @Test
    @DisplayName("Should reject list when project not found")
    void testListByProjectNotFound() {
        when(projectRepository.findById(99L)).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.listByProject(99L, 1L));

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

        StageResponse response = service.get(1L, 1L);

        assertNotNull(response);
        assertEquals("Foundation", response.getName());
        assertEquals(1L, response.getId());
    }

    @Test
    @DisplayName("Should reject get when stage not found")
    void testGetStageNotFound() {
        when(stageRepository.findById(99L)).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.get(99L, 1L));

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
        when(stageRepository.findByConstructionProjectIdAndDisplayOrder(1L, 2))
                .thenReturn(Optional.of(conflictingStage));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.update(1L, updateRequest, 1L));

        assertReasonContains(exception, "Stage with this display order already exists");
    }

    // ============= DELETE TESTS =============
    @Test
    @DisplayName("Should delete stage successfully")
    void testDeleteStageSuccess() {
        Stage stage = new Stage();
        stage.setId(1L);
        stage.setConstructionProject(project);

        when(stageRepository.findById(1L)).thenReturn(Optional.of(stage));

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
        doThrow(new ResponseStatusException(HttpStatus.FORBIDDEN,
                "User does not have permission to perform this operation: MANAGE_STAGES"))
                .when(permissionService).requirePermission(1L, 3L, ProjectPermission.MANAGE_STAGES);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.delete(1L, 3L));

        assertReasonContains(exception, "MANAGE_STAGES");
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

        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));
        when(stageRepository.findByConstructionProjectIdOrderByDisplayOrder(1L))
                .thenReturn(List.of(stage1, stage2));

        service.reorder(1L, reorderedIds, 1L);

        assertEquals(1, stage2.getDisplayOrder());
        assertEquals(2, stage1.getDisplayOrder());
    }

    @Test
    @DisplayName("Should move stages through a temporary range before writing final order")
    void testReorderAvoidsTransientDuplicateOrder() {
        Stage stage1 = new Stage();
        stage1.setId(1L);
        stage1.setDisplayOrder(1);
        stage1.setConstructionProject(project);

        Stage stage2 = new Stage();
        stage2.setId(2L);
        stage2.setDisplayOrder(2);
        stage2.setConstructionProject(project);

        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));
        when(stageRepository.findByConstructionProjectIdOrderByDisplayOrder(1L))
                .thenReturn(List.of(stage1, stage2));

        // Nenhuma etapa pode ser gravada numa posicao ja ocupada por outra que ainda nao
        // foi regravada -- e isso que estourava a unique constraint no Postgres.
        List<Integer> ordersAtSave = new ArrayList<>();
        when(stageRepository.save(any(Stage.class))).thenAnswer(invocation -> {
            Stage saved = invocation.getArgument(0);
            ordersAtSave.add(saved.getDisplayOrder());
            return saved;
        });

        service.reorder(1L, List.of(2L, 1L), 1L);

        // fase 1: faixa temporaria (3, 4); fase 2: ordem final (1, 2)
        assertEquals(List.of(3, 4, 1, 2), ordersAtSave);
        verify(stageRepository, times(2)).flush();
    }

    @Test
    @DisplayName("Should reject reorder when stage not in project")
    void testReorderStageNotInProject() {
        List<Long> reorderedIds = List.of(1L);

        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));
        when(stageRepository.findByConstructionProjectIdOrderByDisplayOrder(1L))
                .thenReturn(List.of());
        when(stageRepository.existsById(1L)).thenReturn(true);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.reorder(1L, reorderedIds, 1L));

        assertReasonContains(exception, "Stage does not belong to this project");
    }

    @Test
    @DisplayName("Should reject reorder when stage not found")
    void testReorderStageNotFound() {
        List<Long> reorderedIds = List.of(99L);

        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));
        when(stageRepository.findByConstructionProjectIdOrderByDisplayOrder(1L))
                .thenReturn(List.of());
        when(stageRepository.existsById(99L)).thenReturn(false);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.reorder(1L, reorderedIds, 1L));

        assertReasonContains(exception, "Stage not found");
    }

    @Test
    @DisplayName("Should reject reorder with duplicate stage ids")
    void testReorderDuplicateIds() {
        Stage stage1 = new Stage();
        stage1.setId(1L);
        stage1.setDisplayOrder(1);
        stage1.setConstructionProject(project);

        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));
        when(stageRepository.findByConstructionProjectIdOrderByDisplayOrder(1L))
                .thenReturn(List.of(stage1));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.reorder(1L, List.of(1L, 1L), 1L));

        assertReasonContains(exception, "duplicate stage ids");
    }

    @Test
    @DisplayName("Should reject reorder that omits stages of the project")
    void testReorderPartialList() {
        Stage stage1 = new Stage();
        stage1.setId(1L);
        stage1.setDisplayOrder(1);
        stage1.setConstructionProject(project);

        Stage stage2 = new Stage();
        stage2.setId(2L);
        stage2.setDisplayOrder(2);
        stage2.setConstructionProject(project);

        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));
        when(stageRepository.findByConstructionProjectIdOrderByDisplayOrder(1L))
                .thenReturn(List.of(stage1, stage2));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.reorder(1L, List.of(2L), 1L));

        assertReasonContains(exception, "must contain all stages of the project");
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
