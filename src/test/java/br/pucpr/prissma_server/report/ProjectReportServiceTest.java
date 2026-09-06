package br.pucpr.prissma_server.report;

import br.pucpr.prissma_server.budget.BudgetItemRepository;
import br.pucpr.prissma_server.budget.ExpenseRepository;
import br.pucpr.prissma_server.budget.ProjectBudgetRepository;
import br.pucpr.prissma_server.projects.ConstructionProject;
import br.pucpr.prissma_server.projects.ConstructionProjectMemberRepository;
import br.pucpr.prissma_server.projects.ConstructionProjectRepository;
import br.pucpr.prissma_server.projects.ProjectPermission;
import br.pucpr.prissma_server.projects.ProjectPermissionService;
import br.pucpr.prissma_server.stage.StageRepository;
import br.pucpr.prissma_server.task.TaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProjectReportService Tests")
class ProjectReportServiceTest {

    @Mock private ConstructionProjectRepository projectRepository;
    @Mock private StageRepository stageRepository;
    @Mock private TaskRepository taskRepository;
    @Mock private ConstructionProjectMemberRepository memberRepository;
    @Mock private ProjectBudgetRepository budgetRepository;
    @Mock private BudgetItemRepository budgetItemRepository;
    @Mock private ExpenseRepository expenseRepository;
    @Mock private ProjectPermissionService permissionService;
    @Mock private ProjectReportPdfRenderer renderer;

    @InjectMocks
    private ProjectReportService service;

    private static final Long PROJECT_ID = 1L;
    private static final Long USER_ID = 7L;

    private ConstructionProject project;

    @BeforeEach
    void setUp() {
        project = new ConstructionProject();
        project.setId(PROJECT_ID);
        project.setTitle("Residência Alfa");
        project.setStatus("IN_PROGRESS");
        project.setProjectType("Residencial");
        project.setCategory("Reforma");
        project.setLandArea(new BigDecimal("300.00"));
        project.setBuiltArea(new BigDecimal("180.00"));
        project.setPlannedStartDate(LocalDate.of(2026, 1, 10));
        project.setPlannedEndDate(LocalDate.of(2026, 12, 20));
    }

    private void stubEmptyProject() {
        when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.of(project));
        when(stageRepository.findByConstructionProjectIdOrderByDisplayOrder(PROJECT_ID)).thenReturn(List.of());
        when(taskRepository.findByStageConstructionProjectIdOrderByCreatedAtAscIdAsc(PROJECT_ID)).thenReturn(List.of());
        when(memberRepository.findAllByConstructionProjectIdOrderByJoinedAtAscIdAsc(PROJECT_ID)).thenReturn(List.of());
        when(budgetRepository.findByConstructionProjectId(PROJECT_ID)).thenReturn(Optional.empty());
    }

    @Test
    @DisplayName("obra inexistente retorna 404")
    void projectNotFound() {
        when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.generate(PROJECT_ID, null, null, USER_ID));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        verify(renderer, never()).render(any());
    }

    @Test
    @DisplayName("sem VIEW_PROJECT retorna 403 e não gera PDF")
    void withoutPermission() {
        when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.of(project));
        doThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "denied"))
                .when(permissionService)
                .requirePermission(PROJECT_ID, USER_ID, ProjectPermission.VIEW_PROJECT);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.generate(PROJECT_ID, null, null, USER_ID));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        verify(renderer, never()).render(any());
    }

    @Test
    @DisplayName("a permissão é checada antes de carregar qualquer dado da obra")
    void permissionIsCheckedBeforeLoadingData() {
        stubEmptyProject();
        when(renderer.render(any())).thenReturn(new byte[]{1});

        service.generate(PROJECT_ID, LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30), USER_ID);

        InOrder order = inOrder(permissionService, stageRepository, taskRepository, budgetRepository);
        order.verify(permissionService).requirePermission(PROJECT_ID, USER_ID, ProjectPermission.VIEW_PROJECT);
        order.verify(stageRepository).findByConstructionProjectIdOrderByDisplayOrder(PROJECT_ID);
        order.verify(taskRepository).findByStageConstructionProjectIdOrderByCreatedAtAscIdAsc(PROJECT_ID);
        order.verify(budgetRepository).findByConstructionProjectId(PROJECT_ID);
    }

    @Test
    @DisplayName("data inicial posterior à final retorna 400")
    void invertedPeriod() {
        when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.of(project));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.generate(PROJECT_ID,
                        LocalDate.of(2026, 6, 30), LocalDate.of(2026, 6, 1), USER_ID));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        verify(renderer, never()).render(any());
    }

    @Test
    @DisplayName("obra sem orçamento gera o relatório mesmo assim, sem consultar itens nem despesas")
    void withoutBudgetStillRenders() {
        stubEmptyProject();
        when(renderer.render(any())).thenReturn(new byte[]{'%', 'P', 'D', 'F'});

        ProjectReportService.ProjectReportPdf pdf =
                service.generate(PROJECT_ID, LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30), USER_ID);

        assertArrayEquals(new byte[]{'%', 'P', 'D', 'F'}, pdf.content());
        verify(budgetItemRepository, never()).findByProjectBudgetIdOrderByIdAsc(anyLong());
        verify(expenseRepository, never()).findAllForReport(anyLong());
    }

    @Test
    @DisplayName("o nome do arquivo carrega a obra e o período resolvido")
    void fileNameCarriesProjectAndPeriod() {
        stubEmptyProject();
        when(renderer.render(any())).thenReturn(new byte[]{1});

        ProjectReportService.ProjectReportPdf pdf =
                service.generate(PROJECT_ID, LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30), USER_ID);

        assertEquals("relatorio-obra-1-2026-06-01-a-2026-06-30.pdf", pdf.fileName());
    }

    @Test
    @DisplayName("sem datas, o período começa no início planejado da obra")
    void defaultsToProjectPlannedStart() {
        stubEmptyProject();
        when(renderer.render(any())).thenReturn(new byte[]{1});

        ProjectReportService.ProjectReportPdf pdf = service.generate(PROJECT_ID, null, null, USER_ID);

        assertTrue(pdf.fileName().contains("2026-01-10"),
                "esperava o plannedStartDate da obra como início: " + pdf.fileName());
    }
}
