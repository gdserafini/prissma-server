package br.pucpr.prissma_server.report;

import br.pucpr.prissma_server.budget.BudgetItem;
import br.pucpr.prissma_server.budget.Expense;
import br.pucpr.prissma_server.budget.ProjectBudget;
import br.pucpr.prissma_server.projects.ConstructionProjectMember;
import br.pucpr.prissma_server.stage.Stage;
import br.pucpr.prissma_server.task.Task;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Cálculo puro do relatório: entidades + período entram, {@link ReportSummary} sai.
 *
 * Sem Spring, sem repositório e sem I/O de propósito — é a única parte da feature
 * com regra de negócio de verdade, e assim ela é testável direto, sem banco.
 *
 * O volume por obra é pequeno (dezenas de etapas, centenas de despesas), então o
 * recorte por período acontece aqui em memória, em vez de virar filtro de data
 * espalhado pelos repositórios.
 */
public final class ReportMetrics {

    /** Espelha TaskService.STAGE_STATUSES/TASK_STATUSES, que são privadas lá. */
    static final String STATUS_DONE = "DONE";

    private static final String MEMBERSHIP_ACTIVE = "ACTIVE";

    /** Teto de linhas da tabela de etapas — o que mantém o PDF em uma página. */
    static final int MAX_STAGE_LINES = 8;

    /** Categorias no gráfico de orçamento. */
    static final int MAX_BUDGET_CATEGORIES = 5;

    private ReportMetrics() {
    }

    public static ReportSummary compute(List<Stage> stages,
                                        List<Task> tasks,
                                        ProjectBudget budget,
                                        List<BudgetItem> items,
                                        List<Expense> expenses,
                                        List<ConstructionProjectMember> members,
                                        ReportPeriod period,
                                        ZoneId zone) {

        int doneStages = 0;
        int stagesDoneInPeriod = 0;
        int lateStages = 0;
        long deviationSum = 0L;
        int deviationCount = 0;

        List<ReportSummary.StageLine> periodStages = new ArrayList<>();

        for (Stage stage : stages) {
            boolean done = STATUS_DONE.equals(stage.getStatus());
            Long deviation = deviationDays(stage);

            if (done) {
                doneStages++;
                if (period.contains(stage.getActualEndDate())) {
                    stagesDoneInPeriod++;
                }
            }
            if (deviation != null) {
                deviationSum += deviation;
                deviationCount++;
            }

            boolean late = isStageLate(stage, done, deviation, period);
            if (late) {
                lateStages++;
            }

            // A tabela mostra o que cruza o período, olhando tanto o planejado
            // quanto o realizado: uma etapa planejada para antes mas concluída
            // dentro da janela precisa aparecer.
            boolean inPeriod = period.overlaps(stage.getPlannedStartDate(), stage.getPlannedEndDate())
                    || period.overlaps(stage.getActualStartDate(), stage.getActualEndDate());
            if (inPeriod) {
                periodStages.add(new ReportSummary.StageLine(
                        stage.getName(),
                        stage.getStatus(),
                        stage.getPlannedEndDate(),
                        stage.getActualEndDate(),
                        deviation,
                        late));
            }
        }

        int totalPeriodStages = periodStages.size();
        List<ReportSummary.StageLine> stageLines = trimStages(periodStages);
        int hiddenStages = totalPeriodStages - stageLines.size();

        int doneTasks = 0;
        int tasksDoneInPeriod = 0;
        int lateTasks = 0;

        for (Task task : tasks) {
            boolean done = STATUS_DONE.equals(task.getStatus());
            if (done) {
                doneTasks++;
                if (period.contains(task.getCompletedAt(), zone)) {
                    tasksDoneInPeriod++;
                }
            }
            if (isTaskLate(task, done, period, zone)) {
                lateTasks++;
            }
        }

        Double averageDeviation = deviationCount == 0
                ? null
                : (double) deviationSum / deviationCount;

        return new ReportSummary(
                stages.size(), doneStages, stagesDoneInPeriod,
                tasks.size(), doneTasks, tasksDoneInPeriod,
                lateStages, lateTasks,
                budget != null,
                budget != null ? budget.getPlannedTotal() : BigDecimal.ZERO,
                sum(expenses, e -> true),
                sum(expenses, e -> period.contains(e.getSpentAt())),
                averageDeviation,
                stageLines,
                hiddenStages,
                categories(items, expenses, period),
                team(members));
    }

    /**
     * Desvio só existe para etapa concluída com as duas datas de fim. Positivo é
     * atraso. Stage é o único ponto do modelo com par planejado/realizado
     * completo — a obra não tem datas reais e a tarefa só tem completedAt.
     */
    static Long deviationDays(Stage stage) {
        if (!STATUS_DONE.equals(stage.getStatus())) {
            return null;
        }
        if (stage.getPlannedEndDate() == null || stage.getActualEndDate() == null) {
            return null;
        }
        return ChronoUnit.DAYS.between(stage.getPlannedEndDate(), stage.getActualEndDate());
    }

    /**
     * Atrasada é a etapa concluída depois do planejado, ou a não concluída cujo
     * prazo já venceu dentro da janela do relatório. Sem prazo cadastrado não há
     * atraso a apontar.
     */
    private static boolean isStageLate(Stage stage, boolean done, Long deviation, ReportPeriod period) {
        if (done) {
            return deviation != null && deviation > 0;
        }
        LocalDate plannedEnd = stage.getPlannedEndDate();
        return plannedEnd != null && plannedEnd.isBefore(period.to());
    }

    private static boolean isTaskLate(Task task, boolean done, ReportPeriod period, ZoneId zone) {
        LocalDate plannedEnd = task.getPlannedEndDate();
        if (plannedEnd == null) {
            return false;
        }
        if (done) {
            return task.getCompletedAt() != null
                    && LocalDate.ofInstant(task.getCompletedAt(), zone).isAfter(plannedEnd);
        }
        return plannedEnd.isBefore(period.to());
    }

    /**
     * Prioriza o que o leitor precisa ver: maiores desvios em módulo primeiro,
     * depois a ordem natural da obra. O corte é o que torna a altura da página
     * previsível independentemente do tamanho da obra.
     */
    private static List<ReportSummary.StageLine> trimStages(List<ReportSummary.StageLine> lines) {
        if (lines.size() <= MAX_STAGE_LINES) {
            return List.copyOf(lines);
        }
        List<ReportSummary.StageLine> sorted = new ArrayList<>(lines);
        sorted.sort(Comparator.comparingLong(
                (ReportSummary.StageLine line) -> line.deviationDays() == null
                        ? Long.MIN_VALUE
                        : Math.abs(line.deviationDays())).reversed());
        return List.copyOf(sorted.subList(0, MAX_STAGE_LINES));
    }

    /**
     * Planejado por categoria vem dos itens de orçamento; executado vem das
     * despesas do período, agrupadas pela categoria do item ao qual pertencem.
     */
    private static List<ReportSummary.BudgetCategoryLine> categories(List<BudgetItem> items,
                                                                     List<Expense> expenses,
                                                                     ReportPeriod period) {
        Map<String, BigDecimal> planned = new LinkedHashMap<>();
        for (BudgetItem item : items) {
            planned.merge(item.getCategory(), item.getPlannedAmount(), BigDecimal::add);
        }

        Map<String, BigDecimal> executed = new LinkedHashMap<>();
        for (Expense expense : expenses) {
            if (period.contains(expense.getSpentAt())) {
                executed.merge(expense.getBudgetItem().getCategory(), expense.getAmount(), BigDecimal::add);
            }
        }

        List<ReportSummary.BudgetCategoryLine> lines = new ArrayList<>();
        for (Map.Entry<String, BigDecimal> entry : planned.entrySet()) {
            BigDecimal spent = executed.getOrDefault(entry.getKey(), BigDecimal.ZERO);
            lines.add(new ReportSummary.BudgetCategoryLine(
                    entry.getKey(),
                    entry.getValue(),
                    spent,
                    spent.compareTo(entry.getValue()) > 0));
        }

        lines.sort(Comparator.comparing(ReportSummary.BudgetCategoryLine::planned).reversed());
        return lines.size() <= MAX_BUDGET_CATEGORIES
                ? List.copyOf(lines)
                : List.copyOf(lines.subList(0, MAX_BUDGET_CATEGORIES));
    }

    /**
     * Só membros ativos, e só nome e papel: o PDF circula fora do sistema, então
     * nada de e-mail ou id de usuário aqui.
     */
    private static List<ReportSummary.TeamLine> team(List<ConstructionProjectMember> members) {
        List<ReportSummary.TeamLine> lines = new ArrayList<>();
        for (ConstructionProjectMember member : members) {
            if (MEMBERSHIP_ACTIVE.equals(member.getMembershipStatus())) {
                lines.add(new ReportSummary.TeamLine(
                        member.getUser().getName(),
                        member.getRoleInProject()));
            }
        }
        return List.copyOf(lines);
    }

    private static BigDecimal sum(List<Expense> expenses, java.util.function.Predicate<Expense> filter) {
        BigDecimal total = BigDecimal.ZERO;
        for (Expense expense : expenses) {
            if (filter.test(expense)) {
                total = total.add(expense.getAmount());
            }
        }
        return total;
    }
}
