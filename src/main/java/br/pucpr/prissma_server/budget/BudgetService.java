package br.pucpr.prissma_server.budget;

import br.pucpr.prissma_server.projects.ConstructionProject;
import br.pucpr.prissma_server.projects.ConstructionProjectRepository;
import br.pucpr.prissma_server.projects.ProjectPermission;
import br.pucpr.prissma_server.projects.ProjectPermissionService;
import br.pucpr.prissma_server.stage.Stage;
import br.pucpr.prissma_server.stage.StageRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class BudgetService {

    private final ProjectBudgetRepository budgetRepository;
    private final BudgetItemRepository itemRepository;
    private final ExpenseRepository expenseRepository;
    private final ConstructionProjectRepository projectRepository;
    private final StageRepository stageRepository;
    private final ProjectPermissionService permissionService;

    public BudgetService(ProjectBudgetRepository budgetRepository,
                         BudgetItemRepository itemRepository,
                         ExpenseRepository expenseRepository,
                         ConstructionProjectRepository projectRepository,
                         StageRepository stageRepository,
                         ProjectPermissionService permissionService) {
        this.budgetRepository = budgetRepository;
        this.itemRepository = itemRepository;
        this.expenseRepository = expenseRepository;
        this.projectRepository = projectRepository;
        this.stageRepository = stageRepository;
        this.permissionService = permissionService;
    }

    /**
     * Garante que o usuário pode acessar o projeto e retorna o vínculo (membership)
     * correspondente. Retorna {@code null} quando o usuário é ADMIN, pois o ADMIN
     * ignora as regras de vínculo/cargo do projeto.
     */
    /** Leitura do orcamento: basta VIEW_PROJECT (ou ADMIN global). */
    private void requireProjectAccess(Long projectId, Long userId) {
        permissionService.requirePermission(projectId, userId, ProjectPermission.VIEW_PROJECT);
    }

    /**
     * Escrita no orcamento: exige MANAGE_BUDGET.
     *
     * A versao anterior comparava o papel contra uma lista fixa
     * (OWNER/ENGINEER/FOREMAN), o que ignorava a customizacao de permissoes por
     * obra que a propria API expoe em PUT /projects/{id}/roles/{role}/permissions.
     */
    private void requireBudgetManager(Long projectId, Long userId) {
        permissionService.requirePermission(projectId, userId, ProjectPermission.MANAGE_BUDGET);
    }

    private ProjectBudget requireBudget(Long budgetId) {
        return budgetRepository.findById(budgetId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Budget not found"));
    }

    private BudgetItem requireItem(Long itemId) {
        return itemRepository.findById(itemId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Budget item not found"));
    }

    private Expense requireExpense(Long expenseId) {
        return expenseRepository.findById(expenseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Expense not found"));
    }

    /**
     * Resolve a etapa (Stage) informada, garantindo que ela pertence ao projeto.
     * Retorna {@code null} quando nenhum stageId é enviado.
     */
    private Stage resolveStage(Long stageId, Long projectId) {
        if (stageId == null) {
            return null;
        }
        Stage stage = stageRepository.findById(stageId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Stage not found"));
        if (!stage.getConstructionProject().getId().equals(projectId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Stage does not belong to this project");
        }
        return stage;
    }

    /**
     * Indica se o valor gasto ultrapassou o valor planejado.
     */
    private boolean isExceeded(BigDecimal spent, BigDecimal planned) {
        return spent.compareTo(planned) > 0;
    }

    @Transactional
    public ProjectBudgetResponse createBudget(Long projectId, ProjectBudgetRequest request, Long userId) {
        ConstructionProject project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found"));

        requireBudgetManager(projectId, userId);

        if (budgetRepository.existsByConstructionProjectId(projectId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Project already has a budget. Update it instead of creating a new one.");
        }

        ProjectBudget budget = BudgetMapper.toEntity(request, project);
        Instant now = Instant.now();
        budget.setCreatedAt(now);
        budget.setUpdatedAt(now);

        ProjectBudget saved = budgetRepository.save(budget);
        return buildBudgetResponse(saved);
    }

    @Transactional(readOnly = true)
    public ProjectBudgetResponse getByProject(Long projectId, Long userId) {
        projectRepository.findById(projectId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found"));
        requireProjectAccess(projectId, userId);

        ProjectBudget budget = budgetRepository.findByConstructionProjectId(projectId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Project does not have a budget yet"));

        return buildBudgetResponse(budget);
    }

    @Transactional(readOnly = true)
    public ProjectBudgetResponse get(Long budgetId, Long userId) {
        ProjectBudget budget = requireBudget(budgetId);
        requireProjectAccess(budget.getConstructionProject().getId(), userId);
        return buildBudgetResponse(budget);
    }

    @Transactional
    public ProjectBudgetResponse updateBudget(Long budgetId, ProjectBudgetRequest request, Long userId) {
        ProjectBudget budget = requireBudget(budgetId);
        requireBudgetManager(budget.getConstructionProject().getId(), userId);

        if (request.getPlannedTotal() != null) {
            budget.setPlannedTotal(request.getPlannedTotal());
        }
        if (request.getDescription() != null) {
            budget.setDescription(request.getDescription());
        }
        budget.setUpdatedAt(Instant.now());

        ProjectBudget saved = budgetRepository.save(budget);
        return buildBudgetResponse(saved);
    }

    @Transactional
    public void deleteBudget(Long budgetId, Long userId) {
        ProjectBudget budget = requireBudget(budgetId);
        requireBudgetManager(budget.getConstructionProject().getId(), userId);
        budgetRepository.delete(budget);
    }

    @Transactional
    public BudgetItemResponse createItem(Long budgetId, BudgetItemRequest request, Long userId) {
        ProjectBudget budget = requireBudget(budgetId);
        requireBudgetManager(budget.getConstructionProject().getId(), userId);

        BudgetItem item = BudgetMapper.toEntity(request, budget);
        BudgetItem saved = itemRepository.save(item);
        touchBudget(budget);
        return BudgetMapper.toResponse(saved, BigDecimal.ZERO);
    }

    @Transactional(readOnly = true)
    public List<BudgetItemResponse> listItems(Long budgetId, Long userId) {
        ProjectBudget budget = requireBudget(budgetId);
        requireProjectAccess(budget.getConstructionProject().getId(), userId);

        return itemRepository.findByProjectBudgetIdOrderByIdAsc(budgetId).stream()
                .map(i -> BudgetMapper.toResponse(i,
                        expenseRepository.sumAmountByBudgetItemId(i.getId())))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public BudgetItemResponse getItem(Long itemId, Long userId) {
        BudgetItem item = requireItem(itemId);
        requireProjectAccess(item.getProjectBudget().getConstructionProject().getId(), userId);
        BigDecimal totalSpent = expenseRepository.sumAmountByBudgetItemId(itemId);
        return BudgetMapper.toResponse(item, totalSpent);
    }

    @Transactional
    public BudgetItemResponse updateItem(Long itemId, BudgetItemRequest request, Long userId) {
        BudgetItem item = requireItem(itemId);
        ProjectBudget budget = item.getProjectBudget();
        requireBudgetManager(budget.getConstructionProject().getId(), userId);

        if (request.getCategory() != null && !request.getCategory().isBlank()) {
            item.setCategory(request.getCategory());
        }
        if (request.getDescription() != null && !request.getDescription().isBlank()) {
            item.setDescription(request.getDescription());
        }
        if (request.getPlannedAmount() != null) {
            item.setPlannedAmount(request.getPlannedAmount());
        }

        BudgetItem saved = itemRepository.save(item);
        touchBudget(budget);
        BigDecimal totalSpent = expenseRepository.sumAmountByBudgetItemId(saved.getId());
        return BudgetMapper.toResponse(saved, totalSpent);
    }

    @Transactional
    public void deleteItem(Long itemId, Long userId) {
        BudgetItem item = requireItem(itemId);
        ProjectBudget budget = item.getProjectBudget();
        requireBudgetManager(budget.getConstructionProject().getId(), userId);
        itemRepository.delete(item);
        touchBudget(budget);
    }

    @Transactional
    public ExpenseResponse createExpense(Long itemId, ExpenseRequest request, Long userId) {
        BudgetItem item = requireItem(itemId);
        ProjectBudget budget = item.getProjectBudget();
        Long projectId = budget.getConstructionProject().getId();
        requireBudgetManager(projectId, userId);

        Stage stage = resolveStage(request.getStageId(), projectId);

        Expense expense = BudgetMapper.toEntity(request, item, stage);
        expense.setCreatedAt(Instant.now());
        Expense saved = expenseRepository.save(expense);

        BigDecimal categorySpent = expenseRepository.sumAmountByBudgetItemId(item.getId());
        BigDecimal budgetSpent = expenseRepository.sumAmountByProjectBudgetId(budget.getId());
        boolean categoryExceeded = isExceeded(categorySpent, item.getPlannedAmount());
        boolean budgetExceeded = isExceeded(budgetSpent, budget.getPlannedTotal());

        touchBudget(budget);
        return BudgetMapper.toResponse(saved, categoryExceeded, budgetExceeded);
    }

    @Transactional(readOnly = true)
    public List<ExpenseResponse> listExpensesByItem(Long itemId, Long userId) {
        BudgetItem item = requireItem(itemId);
        ProjectBudget budget = item.getProjectBudget();
        requireProjectAccess(budget.getConstructionProject().getId(), userId);

        BigDecimal categorySpent = expenseRepository.sumAmountByBudgetItemId(itemId);
        BigDecimal budgetSpent = expenseRepository.sumAmountByProjectBudgetId(budget.getId());
        boolean categoryExceeded = isExceeded(categorySpent, item.getPlannedAmount());
        boolean budgetExceeded = isExceeded(budgetSpent, budget.getPlannedTotal());

        return expenseRepository.findByBudgetItemIdOrderBySpentAtDesc(itemId).stream()
                .map(e -> BudgetMapper.toResponse(e, categoryExceeded, budgetExceeded))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ExpenseResponse> listExpensesByBudget(Long budgetId, Long userId) {
        ProjectBudget budget = requireBudget(budgetId);
        requireProjectAccess(budget.getConstructionProject().getId(), userId);

        BigDecimal budgetSpent = expenseRepository.sumAmountByProjectBudgetId(budgetId);
        boolean budgetExceeded = isExceeded(budgetSpent, budget.getPlannedTotal());

        return expenseRepository.findByProjectBudgetId(budgetId).stream()
                .map(e -> {
                    BigDecimal categorySpent = expenseRepository
                            .sumAmountByBudgetItemId(e.getBudgetItem().getId());
                    boolean categoryExceeded = isExceeded(categorySpent,
                            e.getBudgetItem().getPlannedAmount());
                    return BudgetMapper.toResponse(e, categoryExceeded, budgetExceeded);
                })
                .collect(Collectors.toList());
    }

    @Transactional
    public ExpenseResponse updateExpense(Long expenseId, ExpenseRequest request, Long userId) {
        Expense expense = requireExpense(expenseId);
        BudgetItem item = expense.getBudgetItem();
        ProjectBudget budget = item.getProjectBudget();
        Long projectId = budget.getConstructionProject().getId();
        requireBudgetManager(projectId, userId);

        if (request.getStageId() != null) {
            expense.setStage(resolveStage(request.getStageId(), projectId));
        }
        if (request.getDescription() != null && !request.getDescription().isBlank()) {
            expense.setDescription(request.getDescription());
        }
        if (request.getAmount() != null) {
            expense.setAmount(request.getAmount());
        }
        if (request.getSupplier() != null) {
            expense.setSupplier(request.getSupplier());
        }
        if (request.getReceiptUrl() != null) {
            expense.setReceiptUrl(request.getReceiptUrl());
        }
        if (request.getSpentAt() != null) {
            expense.setSpentAt(request.getSpentAt());
        }

        Expense saved = expenseRepository.save(expense);
        BigDecimal categorySpent = expenseRepository.sumAmountByBudgetItemId(item.getId());
        BigDecimal budgetSpent = expenseRepository.sumAmountByProjectBudgetId(budget.getId());
        boolean categoryExceeded = isExceeded(categorySpent, item.getPlannedAmount());
        boolean budgetExceeded = isExceeded(budgetSpent, budget.getPlannedTotal());

        touchBudget(budget);
        return BudgetMapper.toResponse(saved, categoryExceeded, budgetExceeded);
    }

    @Transactional
    public void deleteExpense(Long expenseId, Long userId) {
        Expense expense = requireExpense(expenseId);
        ProjectBudget budget = expense.getBudgetItem().getProjectBudget();
        requireBudgetManager(budget.getConstructionProject().getId(), userId);
        expenseRepository.delete(expense);
        touchBudget(budget);
    }

    private void touchBudget(ProjectBudget budget) {
        // A entidade já é gerenciada dentro da transação; o dirty checking do JPA
        // persiste a alteração no commit, dispensando um save() explícito.
        budget.setUpdatedAt(Instant.now());
    }

    private ProjectBudgetResponse buildBudgetResponse(ProjectBudget budget) {
        List<BudgetItem> items = itemRepository.findByProjectBudgetIdOrderByIdAsc(budget.getId());
        List<BudgetItemResponse> itemResponses = items.stream()
                .map(i -> BudgetMapper.toResponse(i,
                        expenseRepository.sumAmountByBudgetItemId(i.getId())))
                .collect(Collectors.toList());
        BigDecimal totalSpent = expenseRepository.sumAmountByProjectBudgetId(budget.getId());
        return BudgetMapper.toResponse(budget, itemResponses, totalSpent);
    }
}
