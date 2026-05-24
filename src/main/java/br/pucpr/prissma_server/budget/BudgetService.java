package br.pucpr.prissma_server.budget;

import br.pucpr.prissma_server.projects.ConstructionProject;
import br.pucpr.prissma_server.projects.ConstructionProjectMember;
import br.pucpr.prissma_server.projects.ConstructionProjectMemberRepository;
import br.pucpr.prissma_server.projects.ConstructionProjectRepository;
import br.pucpr.prissma_server.stage.Stage;
import br.pucpr.prissma_server.stage.StageRepository;
import br.pucpr.prissma_server.users.Role;
import br.pucpr.prissma_server.users.User;
import br.pucpr.prissma_server.users.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class BudgetService {

    private static final Set<String> MANAGER_ROLES = Set.of("OWNER", "ENGINEER", "FOREMAN");

    private final ProjectBudgetRepository budgetRepository;
    private final BudgetItemRepository itemRepository;
    private final ExpenseRepository expenseRepository;
    private final ConstructionProjectRepository projectRepository;
    private final ConstructionProjectMemberRepository memberRepository;
    private final StageRepository stageRepository;
    private final UserRepository userRepository;

    public BudgetService(ProjectBudgetRepository budgetRepository,
                         BudgetItemRepository itemRepository,
                         ExpenseRepository expenseRepository,
                         ConstructionProjectRepository projectRepository,
                         ConstructionProjectMemberRepository memberRepository,
                         StageRepository stageRepository,
                         UserRepository userRepository) {
        this.budgetRepository = budgetRepository;
        this.itemRepository = itemRepository;
        this.expenseRepository = expenseRepository;
        this.projectRepository = projectRepository;
        this.memberRepository = memberRepository;
        this.stageRepository = stageRepository;
        this.userRepository = userRepository;
    }

    private User requireUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    private void requireProjectAccess(Long projectId, Long userId) {
        User user = requireUser(userId);
        if (user.getRole() == Role.ADMIN) {
            return;
        }
        ConstructionProjectMember member = memberRepository
                .findByConstructionProjectIdAndUserId(projectId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "User is not a member of this project"));
        if (!"ACTIVE".equals(member.getMembershipStatus())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "User is not an active member of this project");
        }
    }

    private void requireBudgetManager(Long projectId, Long userId) {
        User user = requireUser(userId);
        if (user.getRole() == Role.ADMIN) {
            return;
        }
        ConstructionProjectMember member = memberRepository
                .findByConstructionProjectIdAndUserId(projectId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "User is not a member of this project"));
        if (!"ACTIVE".equals(member.getMembershipStatus())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "User is not an active member of this project");
        }
        if (!MANAGER_ROLES.contains(member.getRoleInProject())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Only OWNER, ENGINEER or FOREMAN can manage the budget");
        }
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

        Stage stage = null;
        if (request.getStageId() != null) {
            stage = stageRepository.findById(request.getStageId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Stage not found"));
            if (!stage.getConstructionProject().getId().equals(projectId)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Stage does not belong to this project");
            }
        }

        Expense expense = BudgetMapper.toEntity(request, item, stage);
        expense.setCreatedAt(Instant.now());
        Expense saved = expenseRepository.save(expense);

        BigDecimal categorySpent = expenseRepository.sumAmountByBudgetItemId(item.getId());
        BigDecimal budgetSpent = expenseRepository.sumAmountByProjectBudgetId(budget.getId());
        boolean categoryExceeded = categorySpent.compareTo(item.getPlannedAmount()) > 0;
        boolean budgetExceeded = budgetSpent.compareTo(budget.getPlannedTotal()) > 0;

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
        boolean categoryExceeded = categorySpent.compareTo(item.getPlannedAmount()) > 0;
        boolean budgetExceeded = budgetSpent.compareTo(budget.getPlannedTotal()) > 0;

        return expenseRepository.findByBudgetItemIdOrderBySpentAtDesc(itemId).stream()
                .map(e -> BudgetMapper.toResponse(e, categoryExceeded, budgetExceeded))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ExpenseResponse> listExpensesByBudget(Long budgetId, Long userId) {
        ProjectBudget budget = requireBudget(budgetId);
        requireProjectAccess(budget.getConstructionProject().getId(), userId);

        BigDecimal budgetSpent = expenseRepository.sumAmountByProjectBudgetId(budgetId);
        boolean budgetExceeded = budgetSpent.compareTo(budget.getPlannedTotal()) > 0;

        return expenseRepository.findByProjectBudgetId(budgetId).stream()
                .map(e -> {
                    BigDecimal categorySpent = expenseRepository
                            .sumAmountByBudgetItemId(e.getBudgetItem().getId());
                    boolean categoryExceeded = categorySpent
                            .compareTo(e.getBudgetItem().getPlannedAmount()) > 0;
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
            Stage stage = stageRepository.findById(request.getStageId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Stage not found"));
            if (!stage.getConstructionProject().getId().equals(projectId)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Stage does not belong to this project");
            }
            expense.setStage(stage);
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
        boolean categoryExceeded = categorySpent.compareTo(item.getPlannedAmount()) > 0;
        boolean budgetExceeded = budgetSpent.compareTo(budget.getPlannedTotal()) > 0;

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
        budget.setUpdatedAt(Instant.now());
        budgetRepository.save(budget);
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
