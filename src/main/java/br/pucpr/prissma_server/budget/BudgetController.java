package br.pucpr.prissma_server.budget;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping
public class BudgetController {

    private final BudgetService service;

    public BudgetController(BudgetService service) {
        this.service = service;
    }

    private Long resolveUserId(Authentication auth) {
        Object principal = auth.getPrincipal();
        if (principal instanceof Long userId) {
            return userId;
        }
        if (principal instanceof Number number) {
            return number.longValue();
        }
        return Long.valueOf(auth.getName());
    }

    @PostMapping("/projects/{projectId}/budget")
    public ResponseEntity<ProjectBudgetResponse> createBudget(
            @PathVariable Long projectId,
            @Valid @RequestBody ProjectBudgetRequest request,
            Authentication auth) {
        ProjectBudgetResponse response = service.createBudget(projectId, request, resolveUserId(auth));
        return ResponseEntity.created(URI.create("/budgets/" + response.getId())).body(response);
    }

    @GetMapping("/projects/{projectId}/budget")
    public ResponseEntity<ProjectBudgetResponse> getProjectBudget(
            @PathVariable Long projectId,
            Authentication auth) {
        return ResponseEntity.ok(service.getByProject(projectId, resolveUserId(auth)));
    }

    @GetMapping("/budgets/{id}")
    public ResponseEntity<ProjectBudgetResponse> getBudget(
            @PathVariable Long id,
            Authentication auth) {
        return ResponseEntity.ok(service.get(id, resolveUserId(auth)));
    }

    @PatchMapping("/budgets/{id}")
    public ResponseEntity<ProjectBudgetResponse> updateBudget(
            @PathVariable Long id,
            @RequestBody ProjectBudgetRequest request,
            Authentication auth) {
        return ResponseEntity.ok(service.updateBudget(id, request, resolveUserId(auth)));
    }

    @DeleteMapping("/budgets/{id}")
    public ResponseEntity<Void> deleteBudget(
            @PathVariable Long id,
            Authentication auth) {
        service.deleteBudget(id, resolveUserId(auth));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/budgets/{budgetId}/items")
    public ResponseEntity<BudgetItemResponse> createItem(
            @PathVariable Long budgetId,
            @Valid @RequestBody BudgetItemRequest request,
            Authentication auth) {
        BudgetItemResponse response = service.createItem(budgetId, request, resolveUserId(auth));
        return ResponseEntity.created(URI.create("/budget-items/" + response.getId())).body(response);
    }

    @GetMapping("/budgets/{budgetId}/items")
    public ResponseEntity<List<BudgetItemResponse>> listItems(
            @PathVariable Long budgetId,
            Authentication auth) {
        return ResponseEntity.ok(service.listItems(budgetId, resolveUserId(auth)));
    }

    @GetMapping("/budget-items/{id}")
    public ResponseEntity<BudgetItemResponse> getItem(
            @PathVariable Long id,
            Authentication auth) {
        return ResponseEntity.ok(service.getItem(id, resolveUserId(auth)));
    }

    @PatchMapping("/budget-items/{id}")
    public ResponseEntity<BudgetItemResponse> updateItem(
            @PathVariable Long id,
            @RequestBody BudgetItemRequest request,
            Authentication auth) {
        return ResponseEntity.ok(service.updateItem(id, request, resolveUserId(auth)));
    }

    @DeleteMapping("/budget-items/{id}")
    public ResponseEntity<Void> deleteItem(
            @PathVariable Long id,
            Authentication auth) {
        service.deleteItem(id, resolveUserId(auth));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/budget-items/{itemId}/expenses")
    public ResponseEntity<ExpenseResponse> createExpense(
            @PathVariable Long itemId,
            @Valid @RequestBody ExpenseRequest request,
            Authentication auth) {
        ExpenseResponse response = service.createExpense(itemId, request, resolveUserId(auth));
        return ResponseEntity.created(URI.create("/expenses/" + response.getId())).body(response);
    }

    @GetMapping("/budget-items/{itemId}/expenses")
    public ResponseEntity<List<ExpenseResponse>> listExpensesByItem(
            @PathVariable Long itemId,
            Authentication auth) {
        return ResponseEntity.ok(service.listExpensesByItem(itemId, resolveUserId(auth)));
    }

    @GetMapping("/budgets/{budgetId}/expenses")
    public ResponseEntity<List<ExpenseResponse>> listExpensesByBudget(
            @PathVariable Long budgetId,
            Authentication auth) {
        return ResponseEntity.ok(service.listExpensesByBudget(budgetId, resolveUserId(auth)));
    }

    @PatchMapping("/expenses/{id}")
    public ResponseEntity<ExpenseResponse> updateExpense(
            @PathVariable Long id,
            @RequestBody ExpenseRequest request,
            Authentication auth) {
        return ResponseEntity.ok(service.updateExpense(id, request, resolveUserId(auth)));
    }

    @DeleteMapping("/expenses/{id}")
    public ResponseEntity<Void> deleteExpense(
            @PathVariable Long id,
            Authentication auth) {
        service.deleteExpense(id, resolveUserId(auth));
        return ResponseEntity.noContent().build();
    }
}
