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

    /**
     * Despesas do orçamento com o item já carregado, para o relatório da obra.
     *
     * O findByProjectBudgetId acima não faz fetch do budgetItem; como o relatório
     * agrupa por categoria e a aplicação roda com open-in-view=false, cada acesso
     * a e.getBudgetItem().getCategory() viraria uma query (N+1) ou uma
     * LazyInitializationException fora da transação.
     */
    @Query("SELECT e FROM Expense e JOIN FETCH e.budgetItem "
            + "WHERE e.budgetItem.projectBudget.id = :budgetId "
            + "ORDER BY e.spentAt ASC, e.id ASC")
    List<Expense> findAllForReport(@Param("budgetId") Long budgetId);
}
