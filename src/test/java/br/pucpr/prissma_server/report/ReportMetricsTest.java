package br.pucpr.prissma_server.report;

import br.pucpr.prissma_server.budget.BudgetItem;
import br.pucpr.prissma_server.budget.Expense;
import br.pucpr.prissma_server.budget.ProjectBudget;
import br.pucpr.prissma_server.projects.ConstructionProjectMember;
import br.pucpr.prissma_server.stage.Stage;
import br.pucpr.prissma_server.task.Task;
import br.pucpr.prissma_server.users.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Fixa as definições do relatório: o que conta como concluído, atrasado e
 * executado dentro do período. É o teste que protege as regras de negócio da
 * feature — o resto do módulo é coleta e formatação.
 */
@DisplayName("ReportMetrics Tests")
class ReportMetricsTest {

    private static final ZoneId ZONE = ZoneId.of("America/Sao_Paulo");
    private static final ReportPeriod JUNHO =
            new ReportPeriod(LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30));

    // ------------------------------------------------------------------ helpers

    private Stage stage(String name, String status, LocalDate plannedEnd, LocalDate actualEnd) {
        return stage(name, status, null, plannedEnd, null, actualEnd);
    }

    private Stage stage(String name, String status,
                        LocalDate plannedStart, LocalDate plannedEnd,
                        LocalDate actualStart, LocalDate actualEnd) {
        Stage stage = new Stage();
        stage.setName(name);
        stage.setStatus(status);
        stage.setDisplayOrder(1);
        stage.setPlannedStartDate(plannedStart);
        stage.setPlannedEndDate(plannedEnd);
        stage.setActualStartDate(actualStart);
        stage.setActualEndDate(actualEnd);
        return stage;
    }

    private Task task(String status, LocalDate plannedEnd, LocalDate completedOn) {
        Task task = new Task();
        task.setStatus(status);
        task.setPlannedEndDate(plannedEnd);
        if (completedOn != null) {
            task.setCompletedAt(completedOn.atTime(LocalTime.NOON).atZone(ZONE).toInstant());
        }
        return task;
    }

    private ProjectBudget budget(String plannedTotal) {
        ProjectBudget budget = new ProjectBudget();
        budget.setPlannedTotal(new BigDecimal(plannedTotal));
        return budget;
    }

    private BudgetItem item(String category, String planned) {
        BudgetItem item = new BudgetItem();
        item.setCategory(category);
        item.setPlannedAmount(new BigDecimal(planned));
        return item;
    }

    private Expense expense(BudgetItem item, String amount, LocalDate spentAt) {
        Expense expense = new Expense();
        expense.setBudgetItem(item);
        expense.setAmount(new BigDecimal(amount));
        expense.setSpentAt(spentAt);
        return expense;
    }

    private ConstructionProjectMember member(String name, String role, String status) {
        User user = new User();
        user.setName(name);
        ConstructionProjectMember member = new ConstructionProjectMember();
        member.setUser(user);
        member.setRoleInProject(role);
        member.setMembershipStatus(status);
        return member;
    }

    private ReportSummary compute(List<Stage> stages, List<Task> tasks) {
        return ReportMetrics.compute(stages, tasks, null, List.of(), List.of(), List.of(), JUNHO, ZONE);
    }

    // ------------------------------------------------------------------ etapas

    @Nested
    @DisplayName("Desvio de dias")
    class Deviation {

        @Test
        @DisplayName("etapa concluída depois do planejado tem desvio positivo e conta como atraso")
        void lateStage() {
            ReportSummary summary = compute(
                    List.of(stage("Fundação", "DONE", LocalDate.of(2026, 6, 10), LocalDate.of(2026, 6, 16))),
                    List.of());

            assertEquals(6L, summary.stages().get(0).deviationDays().longValue());
            assertTrue(summary.stages().get(0).late());
            assertEquals(1, summary.lateStages());
            assertEquals(6.0, summary.averageDeviationDays().doubleValue());
        }

        @Test
        @DisplayName("etapa concluída antes do planejado tem desvio negativo e não é atraso")
        void aheadStage() {
            ReportSummary summary = compute(
                    List.of(stage("Alvenaria", "DONE", LocalDate.of(2026, 6, 20), LocalDate.of(2026, 6, 18))),
                    List.of());

            assertEquals(-2L, summary.stages().get(0).deviationDays().longValue());
            assertFalse(summary.stages().get(0).late());
            assertEquals(0, summary.lateStages());
        }

        @Test
        @DisplayName("etapa concluída sem data planejada não gera desvio nem entra na média")
        void doneWithoutPlannedDate() {
            ReportSummary summary = compute(
                    List.of(stage("Sem prazo", "DONE", null, LocalDate.of(2026, 6, 15))),
                    List.of());

            assertNull(summary.stages().get(0).deviationDays());
            assertEquals(0, summary.lateStages());
            assertNull(summary.averageDeviationDays(), "sem par de datas não há média a reportar");
        }

        @Test
        @DisplayName("etapa não concluída com prazo vencido conta como atraso, mas sem desvio")
        void unfinishedOverdue() {
            ReportSummary summary = compute(
                    List.of(stage("Telhado", "IN_PROGRESS", LocalDate.of(2026, 6, 5), null)),
                    List.of());

            assertNull(summary.stages().get(0).deviationDays());
            assertTrue(summary.stages().get(0).late());
            assertEquals(1, summary.lateStages());
        }

        @Test
        @DisplayName("etapa sem prazo nenhum nunca é atraso")
        void noDatesIsNeverLate() {
            ReportSummary summary = compute(
                    List.of(stage("Livre", "IN_PROGRESS", null, null)),
                    List.of());

            assertEquals(0, summary.lateStages());
        }
    }

    @Nested
    @DisplayName("Recorte por período")
    class Period {

        @Test
        @DisplayName("etapa concluída dentro do período conta em stagesDoneInPeriod, mas o total é da obra")
        void doneInPeriodVersusTotal() {
            ReportSummary summary = compute(
                    List.of(
                            stage("Antiga", "DONE", LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 5)),
                            stage("Do mês", "DONE", LocalDate.of(2026, 6, 10), LocalDate.of(2026, 6, 12))),
                    List.of());

            assertEquals(2, summary.totalStages(), "denominador é sempre a obra inteira");
            assertEquals(2, summary.doneStages());
            assertEquals(1, summary.stagesDoneInPeriod(), "só a concluída dentro da janela");
        }

        @Test
        @DisplayName("etapa sem nenhuma data não aparece na tabela do período")
        void stageWithoutDatesIsNotListed() {
            ReportSummary summary = compute(
                    List.of(stage("Sem datas", "PLANNED", null, null)),
                    List.of());

            assertEquals(1, summary.totalStages());
            assertTrue(summary.stages().isEmpty());
        }

        @Test
        @DisplayName("etapa que apenas cruza a borda do período é listada")
        void overlappingStageIsListed() {
            ReportSummary summary = compute(
                    List.of(stage("Atravessa", "IN_PROGRESS",
                            LocalDate.of(2026, 5, 20), LocalDate.of(2026, 7, 10), null, null)),
                    List.of());

            assertEquals(1, summary.stages().size());
        }
    }

    @Nested
    @DisplayName("Tarefas")
    class Tasks {

        @Test
        @DisplayName("tarefa concluída depois do prazo conta como atrasada")
        void completedLate() {
            ReportSummary summary = compute(List.of(), List.of(
                    task("DONE", LocalDate.of(2026, 6, 10), LocalDate.of(2026, 6, 14))));

            assertEquals(1, summary.doneTasks());
            assertEquals(1, summary.tasksDoneInPeriod());
            assertEquals(1, summary.lateTasks());
        }

        @Test
        @DisplayName("tarefa concluída no prazo não é atraso")
        void completedOnTime() {
            ReportSummary summary = compute(List.of(), List.of(
                    task("DONE", LocalDate.of(2026, 6, 10), LocalDate.of(2026, 6, 10))));

            assertEquals(0, summary.lateTasks());
        }

        @Test
        @DisplayName("tarefa aberta com prazo vencido é atraso; sem prazo, não")
        void openTasks() {
            ReportSummary summary = compute(List.of(), List.of(
                    task("TODO", LocalDate.of(2026, 6, 2), null),
                    task("TODO", null, null)));

            assertEquals(2, summary.totalTasks());
            assertEquals(0, summary.doneTasks());
            assertEquals(1, summary.lateTasks());
        }

        @Test
        @DisplayName("tarefa concluída fora do período não conta no recorte, mas conta no total")
        void completedOutsidePeriod() {
            ReportSummary summary = compute(List.of(), List.of(
                    task("DONE", LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 1))));

            assertEquals(1, summary.doneTasks());
            assertEquals(0, summary.tasksDoneInPeriod());
        }
    }

    @Nested
    @DisplayName("Orçamento")
    class Budget {

        @Test
        @DisplayName("despesas nas bordas exatas do período entram; fora, não")
        void periodBoundariesAreInclusive() {
            BudgetItem material = item("Material", "1000");
            List<Expense> expenses = List.of(
                    expense(material, "100", LocalDate.of(2026, 5, 31)),
                    expense(material, "200", LocalDate.of(2026, 6, 1)),
                    expense(material, "300", LocalDate.of(2026, 6, 30)),
                    expense(material, "400", LocalDate.of(2026, 7, 1)));

            ReportSummary summary = ReportMetrics.compute(List.of(), List.of(),
                    budget("5000"), List.of(material), expenses, List.of(), JUNHO, ZONE);

            assertEquals(0, new BigDecimal("1000").compareTo(summary.executedTotal()),
                    "acumulado ignora o período");
            assertEquals(0, new BigDecimal("500").compareTo(summary.executedInPeriod()),
                    "só 01/06 e 30/06 entram");
        }

        @Test
        @DisplayName("categoria com gasto acima do planejado é marcada como excedida")
        void exceededCategory() {
            BudgetItem material = item("Material", "100");
            ReportSummary summary = ReportMetrics.compute(List.of(), List.of(),
                    budget("100"), List.of(material),
                    List.of(expense(material, "150", LocalDate.of(2026, 6, 10))),
                    List.of(), JUNHO, ZONE);

            assertEquals(1, summary.categories().size());
            assertTrue(summary.categories().get(0).exceeded());
        }

        @Test
        @DisplayName("obra sem orçamento não quebra e reporta hasBudget falso")
        void withoutBudget() {
            ReportSummary summary = compute(List.of(), List.of());

            assertFalse(summary.hasBudget());
            assertEquals(0, BigDecimal.ZERO.compareTo(summary.plannedTotal()));
            assertEquals(0, BigDecimal.ZERO.compareTo(summary.executedTotal()));
            assertTrue(summary.categories().isEmpty());
        }
    }

    @Nested
    @DisplayName("Corte de linhas e equipe")
    class TrimmingAndTeam {

        @Test
        @DisplayName("acima do teto, mantém as de maior desvio e reporta o restante como oculto")
        void trimsToMaxLines() {
            List<Stage> stages = new java.util.ArrayList<>();
            for (int i = 0; i < ReportMetrics.MAX_STAGE_LINES + 4; i++) {
                stages.add(stage("Etapa " + i, "DONE",
                        LocalDate.of(2026, 6, 10),
                        LocalDate.of(2026, 6, 10).plusDays(i)));
            }

            ReportSummary summary = compute(stages, List.of());

            assertEquals(ReportMetrics.MAX_STAGE_LINES, summary.stages().size());
            assertEquals(4, summary.hiddenStages());
            assertEquals(11L, summary.stages().get(0).deviationDays().longValue(), "maior desvio primeiro");
        }

        @Test
        @DisplayName("obra sem etapas não divide por zero")
        void emptyProject() {
            ReportSummary summary = compute(List.of(), List.of());

            assertEquals(0, summary.totalStages());
            assertEquals(0, summary.doneStages());
            assertNull(summary.averageDeviationDays());
            assertEquals(0, summary.hiddenStages());
        }

        @Test
        @DisplayName("equipe traz só membros ativos, com nome e papel")
        void onlyActiveMembers() {
            ReportSummary summary = ReportMetrics.compute(List.of(), List.of(), null, List.of(), List.of(),
                    List.of(member("Ana", "ENGINEER", "ACTIVE"),
                            member("Bruno", "FOREMAN", "INACTIVE")),
                    JUNHO, ZONE);

            assertEquals(1, summary.team().size());
            assertEquals("Ana", summary.team().get(0).name());
            assertEquals("ENGINEER", summary.team().get(0).role());
        }
    }
}
