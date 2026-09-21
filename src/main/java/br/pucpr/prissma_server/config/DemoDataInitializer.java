package br.pucpr.prissma_server.config;

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
import br.pucpr.prissma_server.stage.Stage;
import br.pucpr.prissma_server.stage.StageRepository;
import br.pucpr.prissma_server.task.Task;
import br.pucpr.prissma_server.task.TaskRepository;
import br.pucpr.prissma_server.users.Role;
import br.pucpr.prissma_server.users.User;
import br.pucpr.prissma_server.users.UserRepository;
import br.pucpr.prissma_server.workspaces.Workspace;
import br.pucpr.prissma_server.workspaces.WorkspaceMember;
import br.pucpr.prissma_server.workspaces.WorkspaceMemberRepository;
import br.pucpr.prissma_server.workspaces.WorkspaceRepository;
import br.pucpr.prissma_server.workspaces.WorkspaceRole;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Component
public class DemoDataInitializer implements ApplicationRunner {

    private final UserRepository userRepository;
    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final ConstructionProjectRepository constructionProjectRepository;
    private final ConstructionProjectMemberRepository constructionProjectMemberRepository;
    private final StageRepository stageRepository;
    private final ProjectBudgetRepository projectBudgetRepository;
    private final BudgetItemRepository budgetItemRepository;
    private final ExpenseRepository expenseRepository;
    private final TaskRepository taskRepository;
    private final PasswordEncoder passwordEncoder;

    public DemoDataInitializer(UserRepository userRepository,
                              WorkspaceRepository workspaceRepository,
                              WorkspaceMemberRepository workspaceMemberRepository,
                              ConstructionProjectRepository constructionProjectRepository,
                              ConstructionProjectMemberRepository constructionProjectMemberRepository,
                              StageRepository stageRepository,
                              ProjectBudgetRepository projectBudgetRepository,
                              BudgetItemRepository budgetItemRepository,
                              ExpenseRepository expenseRepository,
                              TaskRepository taskRepository,
                              PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.workspaceRepository = workspaceRepository;
        this.workspaceMemberRepository = workspaceMemberRepository;
        this.constructionProjectRepository = constructionProjectRepository;
        this.constructionProjectMemberRepository = constructionProjectMemberRepository;
        this.stageRepository = stageRepository;
        this.projectBudgetRepository = projectBudgetRepository;
        this.budgetItemRepository = budgetItemRepository;
        this.expenseRepository = expenseRepository;
        this.taskRepository = taskRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (constructionProjectRepository.findAll().stream()
                .anyMatch(project -> "Residencial Demo".equals(project.getTitle()))) {
            return;
        }

        User admin = ensureUser("Admin", "admin@admin.com", "adminadmin", Role.ADMIN);
        User joao = ensureUser("João Almeida", "joao@prissma.com", "123456", Role.ENG);
        User maria = ensureUser("Maria Souza", "maria@prissma.com", "123456", Role.ARQ);
        User pedro = ensureUser("Pedro Lima", "pedro@prissma.com", "123456", Role.USER);
        User carla = ensureUser("Carla Nunes", "carla@prissma.com", "123456", Role.USER);

        Workspace workspace = workspaceRepository.findByOwnerIdAndPrimaryTrueAndDeletedAtIsNull(admin.getId())
                .orElseGet(() -> createWorkspace(admin.getId(), "Obra Demonstrativa"));

        ensureWorkspaceMember(workspace.getId(), admin.getId(), WorkspaceRole.OWNER);
        ensureWorkspaceMember(workspace.getId(), joao.getId(), WorkspaceRole.ADMIN);
        ensureWorkspaceMember(workspace.getId(), maria.getId(), WorkspaceRole.MEMBER);
        ensureWorkspaceMember(workspace.getId(), pedro.getId(), WorkspaceRole.MEMBER);
        ensureWorkspaceMember(workspace.getId(), carla.getId(), WorkspaceRole.CLIENT);

        ConstructionProject project = constructionProjectRepository.findAllByWorkspaceIdOrderByIdAsc(workspace.getId())
                .stream()
                .filter(p -> "Residencial Demo".equals(p.getTitle()))
                .findFirst()
                .orElseGet(() -> createProject(workspace, admin));

        ensureProjectMember(project, admin, "OWNER");
        ensureProjectMember(project, joao, "ENGINEER");
        ensureProjectMember(project, maria, "ARCHITECT");
        ensureProjectMember(project, pedro, "FOREMAN");
        ensureProjectMember(project, carla, "USER");

        Stage fundacao = ensureStage(project, 1, "Fundação", "Preparação e concretagem da base");
        Stage estrutura = ensureStage(project, 2, "Estrutura", "Pilares, vigas e lajes");
        Stage revestimento = ensureStage(project, 3, "Revestimento", "Paredes e acabamento interno");
        Stage acabamento = ensureStage(project, 4, "Acabamento", "Pintura, pisos e detalhes finais");

        ensureTask(fundacao, joao, "Verificar sondagem", "Confirmar resistência do solo antes do início da fundação.", "HIGH", "IN_PROGRESS");
        ensureTask(fundacao, pedro, "Preparar formas", "Montar fôrmas para blocos e vigas do nível térreo.", "MEDIUM", "TODO");
        ensureTask(estrutura, joao, "Concretar pilares", "Concretar pilares do bloco principal da residência.", "HIGH", "DONE");
        ensureTask(estrutura, maria, "Revisar detalhes de estrutura", "Validar alinhamento e reforço das vigas.", "MEDIUM", "IN_PROGRESS");
        ensureTask(revestimento, pedro, "Executar assentamento", "Assentar blocos e controlar alinhamento das paredes.", "MEDIUM", "TODO");
        ensureTask(acabamento, carla, "Aprovar acabamento", "Finalizar escolha de tintas e materiais dos pisos.", "LOW", "TODO");

        ProjectBudget budget = projectBudgetRepository.findByConstructionProjectId(project.getId())
                .orElseGet(() -> createBudget(project));

        BudgetItem materiais = ensureBudgetItem(budget, "Materiais", "Concreto, aço e blocos", new BigDecimal("75000.00"));
        BudgetItem acabamentoItem = ensureBudgetItem(budget, "Acabamento", "Tintas, pisos e louças", new BigDecimal("42000.00"));
        BudgetItem infraestrutura = ensureBudgetItem(budget, "Infraestrutura", "Serviços elétricos e hidráulicos", new BigDecimal("35000.00"));

        ensureExpense(materiais, fundacao, "Entrega de concreto do pavimento", new BigDecimal("18000.00"), "Concretex", LocalDate.now().minusDays(12));
        ensureExpense(materiais, estrutura, "Aço para estrutura", new BigDecimal("22000.00"), "Metal Forte", LocalDate.now().minusDays(8));
        ensureExpense(acabamentoItem, acabamento, "Tinta e acabamento", new BigDecimal("9500.00"), "Pinturas Brasil", LocalDate.now().minusDays(4));
        ensureExpense(infraestrutura, revestimento, "Tubos e conexões hidráulicas", new BigDecimal("7600.00"), "Nilo Instalações", LocalDate.now().minusDays(6));
    }

    private User ensureUser(String name, String email, String rawPassword, Role role) {
        return userRepository.findByEmail(email)
                .orElseGet(() -> userRepository.save(new User(name, email, passwordEncoder.encode(rawPassword), role)));
    }

    private Workspace createWorkspace(Long ownerId, String name) {
        Instant now = Instant.now();
        Workspace workspace = new Workspace();
        workspace.setOwnerId(ownerId);
        workspace.setName(name);
        workspace.setPrimary(true);
        workspace.setCreatedAt(now);
        workspace.setUpdatedAt(now);
        return workspaceRepository.save(workspace);
    }

    private void ensureWorkspaceMember(Long workspaceId, Long userId, WorkspaceRole role) {
        if (workspaceMemberRepository.findByWorkspaceIdAndUserIdAndDeletedAtIsNull(workspaceId, userId).isEmpty()) {
            Instant now = Instant.now();
            WorkspaceMember member = new WorkspaceMember();
            member.setWorkspace(workspaceRepository.findById(workspaceId).orElseThrow());
            member.setUserId(userId);
            member.setRole(role);
            member.setAcceptedAt(now);
            member.setCreatedAt(now);
            member.setUpdatedAt(now);
            member.setActive(true);
            workspaceMemberRepository.save(member);
        }
    }

    private ConstructionProject createProject(Workspace workspace, User owner) {
        Instant now = Instant.now();
        ConstructionProject project = new ConstructionProject();
        project.setTitle("Residencial Demo");
        project.setWorkspaceId(workspace.getId());
        project.setCep("01000-000");
        project.setStreet("Rua do Sol");
        project.setCity("Curitiba");
        project.setState("PR");
        project.setNumber("123");
        project.setComplement("Bloco A");
        project.setProjectType("RESIDENCIAL");
        project.setCategory("HABITACIONAL");
        project.setLandArea(new BigDecimal("450.00"));
        project.setBuiltArea(new BigDecimal("320.00"));
        project.setStatus("IN_PROGRESS");
        project.setPlannedStartDate(LocalDate.now().minusDays(30));
        project.setPlannedEndDate(LocalDate.now().plusDays(120));
        project.setCreatedAt(now);
        project.setUpdatedAt(now);
        return constructionProjectRepository.save(project);
    }

    private void ensureProjectMember(ConstructionProject project, User user, String role) {
        if (constructionProjectMemberRepository.findByConstructionProjectIdAndUserId(project.getId(), user.getId()).isEmpty()) {
            ConstructionProjectMember member = new ConstructionProjectMember();
            member.setConstructionProject(project);
            member.setUser(user);
            member.setRoleInProject(role);
            member.setMembershipStatus("ACTIVE");
            member.setJoinedAt(Instant.now());
            constructionProjectMemberRepository.save(member);
        }
    }

    private Stage ensureStage(ConstructionProject project, int order, String name, String description) {
        return stageRepository.findByConstructionProjectIdAndDisplayOrder(project.getId(), order)
                .orElseGet(() -> {
                    Stage stage = new Stage();
                    stage.setConstructionProject(project);
                    stage.setName(name);
                    stage.setDescription(description);
                    stage.setDisplayOrder(order);
                    stage.setStatus("IN_PROGRESS");
                    stage.setPlannedStartDate(LocalDate.now().minusDays(10 + order));
                    stage.setPlannedEndDate(LocalDate.now().plusDays(20 + order));
                    stage.setCreatedAt(Instant.now());
                    stage.setUpdatedAt(Instant.now());
                    return stageRepository.save(stage);
                });
    }

    private void ensureTask(Stage stage, User assignee, String title, String description, String priority, String status) {
        boolean exists = taskRepository.findByStageIdOrderByCreatedAtAscIdAsc(stage.getId())
                .stream()
                .anyMatch(task -> title.equals(task.getTitle()));

        if (!exists) {
            Task task = new Task();
            task.setStage(stage);
            task.setAssigneeUser(assignee);
            task.setTitle(title);
            task.setDescription(description);
            task.setPriority(priority);
            task.setStatus(status);
            task.setPlannedStartDate(LocalDate.now().minusDays(3));
            task.setPlannedEndDate(LocalDate.now().plusDays(7));
            task.setCreatedAt(Instant.now());
            task.setUpdatedAt(Instant.now());
            if ("DONE".equals(status)) {
                task.setCompletedAt(Instant.now());
            }
            taskRepository.save(task);
        }
    }

    private ProjectBudget createBudget(ConstructionProject project) {
        Instant now = Instant.now();
        ProjectBudget budget = new ProjectBudget();
        budget.setConstructionProject(project);
        budget.setDescription("Orçamento inicial da obra demo");
        budget.setPlannedTotal(new BigDecimal("150000.00"));
        budget.setCreatedAt(now);
        budget.setUpdatedAt(now);
        return projectBudgetRepository.save(budget);
    }

    private BudgetItem ensureBudgetItem(ProjectBudget budget, String category, String description, BigDecimal plannedAmount) {
        return budgetItemRepository.findByProjectBudgetIdOrderByIdAsc(budget.getId()).stream()
                .filter(item -> category.equals(item.getCategory()))
                .findFirst()
                .orElseGet(() -> {
                    BudgetItem item = new BudgetItem();
                    item.setProjectBudget(budget);
                    item.setCategory(category);
                    item.setDescription(description);
                    item.setPlannedAmount(plannedAmount);
                    return budgetItemRepository.save(item);
                });
    }

    private void ensureExpense(BudgetItem budgetItem, Stage stage, String description, BigDecimal amount, String supplier, LocalDate spentAt) {
        if (expenseRepository.findByBudgetItemIdOrderBySpentAtAscIdAsc(budgetItem.getId()).stream()
                .noneMatch(expense -> description.equals(expense.getDescription()))) {
            Expense expense = new Expense();
            expense.setBudgetItem(budgetItem);
            expense.setStage(stage);
            expense.setDescription(description);
            expense.setAmount(amount);
            expense.setSupplier(supplier);
            expense.setSpentAt(spentAt);
            expense.setCreatedAt(Instant.now());
            expenseRepository.save(expense);
        }
    }
}
