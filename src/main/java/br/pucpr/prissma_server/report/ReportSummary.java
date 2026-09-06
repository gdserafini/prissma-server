package br.pucpr.prissma_server.report;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Números crus do relatório, já recortados pelo período — sem nenhuma formatação.
 *
 * Produzido por {@link ReportMetrics} e consumido por {@link ProjectReportView},
 * que é quem transforma isso em texto pt-BR. A separação existe para os testes
 * poderem fixar as regras de contagem sem depender de locale.
 */
public record ReportSummary(

        int totalStages,
        int doneStages,
        int stagesDoneInPeriod,

        int totalTasks,
        int doneTasks,
        int tasksDoneInPeriod,

        int lateStages,
        int lateTasks,

        boolean hasBudget,
        BigDecimal plannedTotal,
        BigDecimal executedTotal,
        BigDecimal executedInPeriod,

        /** Média de desvio em dias; null quando nenhuma etapa concluída tem o par de datas. */
        Double averageDeviationDays,

        /** Etapas do período já ordenadas e cortadas para caber na página. */
        List<StageLine> stages,
        /** Quantas etapas do período ficaram de fora do corte acima. */
        int hiddenStages,

        List<BudgetCategoryLine> categories,
        List<TeamLine> team
) {

    /**
     * @param deviationDays diferença entre o fim real e o fim planejado. Positivo
     *                      é atraso, negativo é adiantamento, null quando a etapa
     *                      não está concluída ou falta uma das datas.
     */
    public record StageLine(
            String name,
            String status,
            LocalDate plannedEndDate,
            LocalDate actualEndDate,
            Long deviationDays,
            boolean late
    ) {}

    public record BudgetCategoryLine(
            String category,
            BigDecimal planned,
            BigDecimal executed,
            boolean exceeded
    ) {}

    public record TeamLine(String name, String role) {}
}
