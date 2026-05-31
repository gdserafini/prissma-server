package br.pucpr.prissma_server.budget;

import br.pucpr.prissma_server.projects.ConstructionProject;
import br.pucpr.prissma_server.stage.Stage;

import java.math.BigDecimal;
import java.util.List;

public class BudgetMapper {

    private BudgetMapper() {}

    public static ProjectBudget toEntity(ProjectBudgetRequest request, ConstructionProject project) {
        ProjectBudget budget = new ProjectBudget();
        budget.setConstructionProject(project);
        budget.setDescription(request.getDescription());
        budget.setPlannedTotal(request.getPlannedTotal());
        return budget;
    }

    public static BudgetItem toEntity(BudgetItemRequest request, ProjectBudget budget) {
        BudgetItem item = new BudgetItem();
        item.setProjectBudget(budget);
        item.setCategory(request.getCategory());
        item.setDescription(request.getDescription());
        item.setPlannedAmount(request.getPlannedAmount());
        return item;
    }

    public static Expense toEntity(ExpenseRequest request, BudgetItem item, Stage stage) {
        Expense expense = new Expense();
        expense.setBudgetItem(item);
        expense.setStage(stage);
        expense.setDescription(request.getDescription());
        expense.setAmount(request.getAmount());
        expense.setSupplier(request.getSupplier());
        expense.setReceiptUrl(request.getReceiptUrl());
        expense.setSpentAt(request.getSpentAt());
        return expense;
    }

    public static BudgetItemResponse toResponse(BudgetItem item, BigDecimal totalSpent) {
        BigDecimal planned = item.getPlannedAmount();
        BigDecimal remaining = planned.subtract(totalSpent);
        boolean exceeded = totalSpent.compareTo(planned) > 0;
        return new BudgetItemResponse(
                item.getId(),
                item.getProjectBudget().getId(),
                item.getCategory(),
                item.getDescription(),
                planned,
                totalSpent,
                remaining,
                exceeded
        );
    }

    public static ProjectBudgetResponse toResponse(ProjectBudget budget,
                                                   List<BudgetItemResponse> items,
                                                   BigDecimal totalSpent) {
        BigDecimal planned = budget.getPlannedTotal();
        BigDecimal remaining = planned.subtract(totalSpent);
        boolean exceeded = totalSpent.compareTo(planned) > 0;
        return new ProjectBudgetResponse(
                budget.getId(),
                budget.getConstructionProject().getId(),
                budget.getDescription(),
                planned,
                totalSpent,
                remaining,
                exceeded,
                budget.getCreatedAt(),
                budget.getUpdatedAt(),
                items
        );
    }

    public static ExpenseResponse toResponse(Expense expense,
                                             boolean categoryExceeded,
                                             boolean budgetExceeded) {
        return new ExpenseResponse(
                expense.getId(),
                expense.getBudgetItem().getId(),
                expense.getStage() != null ? expense.getStage().getId() : null,
                expense.getDescription(),
                expense.getAmount(),
                expense.getSupplier(),
                expense.getReceiptUrl(),
                expense.getSpentAt(),
                expense.getCreatedAt(),
                categoryExceeded,
                budgetExceeded
        );
    }
}
