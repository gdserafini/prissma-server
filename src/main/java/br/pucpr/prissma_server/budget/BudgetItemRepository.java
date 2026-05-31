package br.pucpr.prissma_server.budget;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BudgetItemRepository extends JpaRepository<BudgetItem, Long> {

    List<BudgetItem> findByProjectBudgetIdOrderByIdAsc(Long projectBudgetId);
}
