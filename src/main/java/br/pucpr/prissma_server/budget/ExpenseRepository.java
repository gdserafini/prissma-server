package br.pucpr.prissma_server.budget;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

public interface ExpenseRepository extends JpaRepository<Expense, Long> {

    List<Expense> findByBudgetItemIdOrderBySpentAtDesc(Long budgetItemId);

    @Query("SELECT e FROM Expense e WHERE e.budgetItem.projectBudget.id = :budgetId ORDER BY e.spentAt DESC")
    List<Expense> findByProjectBudgetId(@Param("budgetId") Long budgetId);

    @Query("SELECT COALESCE(SUM(e.amount), 0) FROM Expense e WHERE e.budgetItem.id = :itemId")
    BigDecimal sumAmountByBudgetItemId(@Param("itemId") Long itemId);

    @Query("SELECT COALESCE(SUM(e.amount), 0) FROM Expense e WHERE e.budgetItem.projectBudget.id = :budgetId")
    BigDecimal sumAmountByProjectBudgetId(@Param("budgetId") Long budgetId);
}
