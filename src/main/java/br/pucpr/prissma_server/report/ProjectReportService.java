package br.pucpr.prissma_server.report;

import br.pucpr.prissma_server.budget.BudgetItem;
import br.pucpr.prissma_server.budget.BudgetItemRepository;
import br.pucpr.prissma_server.budget.Expense;
import br.pucpr.prissma_server.budget.ExpenseRepository;
import br.pucpr.prissma_server.budget.ProjectBudget;
import br.pucpr.prissma_server.budget.ProjectBudgetRepository;
import br.pucpr.prissma_server.projects.ConstructionProject;
import br.pucpr.prissma_server.projects.ConstructionProjectMember;
import br.pucpr.prissma_server.projects.ConstructionProjectMemberRepository;
import br.pucpr.prissma_server.projects.ConstructionProjectRepository;
import br.pucpr.prissma_server.projects.ProjectPermission;
import br.pucpr.prissma_server.projects.ProjectPermissionService;
import br.pucpr.prissma_server.stage.Stage;
import br.pucpr.prissma_server.stage.StageRepository;
import br.pucpr.prissma_server.task.Task;
import br.pucpr.prissma_server.task.TaskRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

/**
 * Relatório executivo da obra em PDF (RF: relatórios de obra).
 *
 * Uma página A4 com dados da obra, progresso de etapas e tarefas, orçamento
 * planejado vs. executado, desvio de dias, atrasos e equipe, recortados por um
 * intervalo de datas.
 */
@Service
public class ProjectReportService {

    private final ConstructionProjectRepository projectRepository;
    private final StageRepository stageRepository;
    private final TaskRepository taskRepository;
    private final ConstructionProjectMemberRepository memberRepository;
    private final ProjectBudgetRepository budgetRepository;
    private final BudgetItemRepository budgetItemRepository;
    private final ExpenseRepository expenseRepository;
    private final ProjectPermissionService permissionService;
    private final ProjectReportPdfRenderer renderer;

    public ProjectReportService(ConstructionProjectRepository projectRepository,
                                StageRepository stageRepository,
                                TaskRepository taskRepository,
                                ConstructionProjectMemberRepository memberRepository,
                                ProjectBudgetRepository budgetRepository,
                                BudgetItemRepository budgetItemRepository,
                                ExpenseRepository expenseRepository,
                                ProjectPermissionService permissionService,
                                ProjectReportPdfRenderer renderer) {
        this.projectRepository = projectRepository;
        this.stageRepository = stageRepository;
        this.taskRepository = taskRepository;
        this.memberRepository = memberRepository;
        this.budgetRepository = budgetRepository;
        this.budgetItemRepository = budgetItemRepository;
        this.expenseRepository = expenseRepository;
        this.permissionService = permissionService;
        this.renderer = renderer;
    }

    /**
     * Tudo acontece dentro desta transação de leitura: a aplicação roda com
     * open-in-view=false, então o carregamento LAZY precisa terminar aqui, antes
     * de a renderização começar.
     */
    @Transactional(readOnly = true)
    public ProjectReportPdf generate(Long projectId, LocalDate from, LocalDate to, Long userId) {
        ConstructionProject project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found"));

        // Leitura do relatório = leitura da obra. Todo papel de obra tem
        // VIEW_PROJECT, inclusive o cliente (USER), que é justamente quem mais
        // costuma receber este PDF.
        permissionService.requirePermission(projectId, userId, ProjectPermission.VIEW_PROJECT);

        LocalDate today = LocalDate.now();
        ReportPeriod period = ReportPeriod.resolve(from, to, project.getPlannedStartDate(), today);

        List<Stage> stages = stageRepository.findByConstructionProjectIdOrderByDisplayOrder(projectId);
        List<Task> tasks = taskRepository.findByStageConstructionProjectIdOrderByCreatedAtAscIdAsc(projectId);
        List<ConstructionProjectMember> members =
                memberRepository.findAllByConstructionProjectIdOrderByJoinedAtAscIdAsc(projectId);

        // A obra pode não ter orçamento cadastrado: o relatório continua saindo,
        // só sem o bloco financeiro.
        ProjectBudget budget = budgetRepository.findByConstructionProjectId(projectId).orElse(null);
        List<BudgetItem> items = budget == null
                ? List.of()
                : budgetItemRepository.findByProjectBudgetIdOrderByIdAsc(budget.getId());
        List<Expense> expenses = budget == null
                ? List.of()
                : expenseRepository.findAllForReport(budget.getId());

        ReportSummary summary = ReportMetrics.compute(
                stages, tasks, budget, items, expenses, members, period, ZoneId.systemDefault());

        ProjectReportView view = ProjectReportView.from(
                project, summary, period, today, ProjectReportPdfRenderer.PT_BR);

        return new ProjectReportPdf(renderer.render(view), fileName(projectId, period));
    }

    /** Nome montado só com id e datas — nada vindo do usuário entra aqui. */
    private String fileName(Long projectId, ReportPeriod period) {
        return "relatorio-obra-" + projectId + "-" + period.from() + "-a-" + period.to() + ".pdf";
    }

    public record ProjectReportPdf(byte[] content, String fileName) {}
}
