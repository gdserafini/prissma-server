package br.pucpr.prissma_server.report;

import br.pucpr.prissma_server.projects.ConstructionProject;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * O relatório já formatado em pt-BR: tudo que o template faz é interpolar.
 *
 * Formatar aqui, e não no template, mantém o XHTML limpo (o renderer exige XML
 * bem formado) e deixa a formatação testável sem gerar PDF nenhum.
 */
public record ProjectReportView(

        String title,
        String periodLabel,
        String issuedAtLabel,

        String address,
        String projectType,
        String category,
        String landArea,
        String builtArea,
        String status,
        String plannedWindow,

        String stagesValue,
        String stagesCaption,
        String tasksValue,
        String tasksCaption,
        String executedValue,
        String executedCaption,
        String deviationValue,
        String deviationCaption,

        Donut donut,
        String donutPercent,
        String donutCaption,

        boolean hasBudget,
        String plannedTotal,
        String executedTotal,
        List<CategoryBar> categories,

        List<Bar> deviationBars,
        List<StageRow> stages,
        String hiddenStagesLabel,

        List<String> team,
        String lateSummary
) {

    /**
     * Anel de progresso. Valores como String em Locale.US: formatados pelo
     * template virariam "52,00" em pt-BR, o que é atributo SVG inválido.
     */
    public record Donut(boolean hasProgress, String radius, String dashArray) {}

    /** Uma barra do gráfico de desvio, em coordenadas do viewBox. */
    public record Bar(String x, String y, String width, String height, String color) {}

    /** Larguras já em porcentagem — o renderer não tem calc(). */
    public record CategoryBar(String name,
                              String plannedLabel,
                              String executedLabel,
                              int plannedWidth,
                              int executedWidth,
                              boolean exceeded) {}

    public record StageRow(String name,
                           String status,
                           String plannedEnd,
                           String actualEnd,
                           String deviation,
                           boolean late) {}

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final String EMPTY = "—"; // travessão, dentro do WinAnsi

    public static ProjectReportView from(ConstructionProject project,
                                         ReportSummary summary,
                                         ReportPeriod period,
                                         LocalDate issuedAt,
                                         Locale locale) {

        NumberFormat money = NumberFormat.getCurrencyInstance(locale);

        String stagesCaption = summary.stagesDoneInPeriod() + " concluída(s) no período";
        String tasksCaption = summary.tasksDoneInPeriod() + " concluída(s) no período";

        String deviationValue;
        String deviationCaption;
        if (summary.averageDeviationDays() == null) {
            deviationValue = EMPTY;
            deviationCaption = "sem etapa concluída com prazo";
        } else {
            double average = summary.averageDeviationDays();
            long rounded = Math.round(average);
            deviationValue = (rounded > 0 ? "+" : "") + rounded + (Math.abs(rounded) == 1 ? " dia" : " dias");
            deviationCaption = average > 0 ? "atraso médio por etapa"
                    : (average < 0 ? "adiantamento médio por etapa" : "no prazo");
        }

        int donutPercent = summary.totalStages() == 0
                ? 0
                : (int) Math.round(100.0 * summary.doneStages() / summary.totalStages());

        return new ProjectReportView(
                project.getTitle(),
                "Período de " + period.from().format(DATE) + " a " + period.to().format(DATE),
                "Emitido em " + issuedAt.format(DATE),

                address(project),
                nullToDash(project.getProjectType()),
                nullToDash(project.getCategory()),
                area(project.getLandArea()),
                area(project.getBuiltArea()),
                statusLabel(project.getStatus()),
                plannedWindow(project),

                summary.doneStages() + " / " + summary.totalStages(),
                stagesCaption,
                summary.doneTasks() + " / " + summary.totalTasks(),
                tasksCaption,
                money.format(summary.executedInPeriod()),
                "de " + money.format(summary.executedTotal()) + " acumulados",
                deviationValue,
                deviationCaption,

                ReportCharts.donut(summary.doneStages(), summary.totalStages()),
                donutPercent + "%",
                summary.doneStages() + " de " + summary.totalStages() + " etapas",

                summary.hasBudget(),
                money.format(summary.plannedTotal()),
                money.format(summary.executedTotal()),
                categoryBars(summary, money),

                ReportCharts.deviationBars(summary.stages()),
                stageRows(summary),
                summary.hiddenStages() > 0 ? "+ " + summary.hiddenStages() + " etapa(s) no período" : null,

                teamLabels(summary),
                lateSummary(summary));
    }

    private static List<CategoryBar> categoryBars(ReportSummary summary, NumberFormat money) {
        // Escala comum entre categorias: a maior barra da página é o teto, senão
        // categorias pequenas apareceriam do mesmo tamanho das grandes.
        BigDecimal ceiling = BigDecimal.ZERO;
        for (ReportSummary.BudgetCategoryLine line : summary.categories()) {
            ceiling = ceiling.max(line.planned()).max(line.executed());
        }

        List<CategoryBar> bars = new ArrayList<>();
        for (ReportSummary.BudgetCategoryLine line : summary.categories()) {
            bars.add(new CategoryBar(
                    line.category(),
                    money.format(line.planned()),
                    money.format(line.executed()),
                    percentOf(line.planned(), ceiling),
                    percentOf(line.executed(), ceiling),
                    line.exceeded()));
        }
        return bars;
    }

    private static int percentOf(BigDecimal value, BigDecimal ceiling) {
        if (ceiling == null || ceiling.signum() <= 0 || value == null || value.signum() <= 0) {
            return 0;
        }
        return value.multiply(BigDecimal.valueOf(100))
                .divide(ceiling, 0, RoundingMode.HALF_UP)
                .min(BigDecimal.valueOf(100))
                .intValue();
    }

    private static List<StageRow> stageRows(ReportSummary summary) {
        List<StageRow> rows = new ArrayList<>();
        for (ReportSummary.StageLine line : summary.stages()) {
            String deviation;
            if (line.deviationDays() == null) {
                deviation = EMPTY;
            } else {
                long days = line.deviationDays();
                deviation = (days > 0 ? "+" : "") + days + (Math.abs(days) == 1 ? " dia" : " dias");
            }
            rows.add(new StageRow(
                    line.name(),
                    statusLabel(line.status()),
                    line.plannedEndDate() == null ? EMPTY : line.plannedEndDate().format(DATE),
                    line.actualEndDate() == null ? EMPTY : line.actualEndDate().format(DATE),
                    deviation,
                    line.late()));
        }
        return rows;
    }

    private static List<String> teamLabels(ReportSummary summary) {
        List<String> labels = new ArrayList<>();
        for (ReportSummary.TeamLine member : summary.team()) {
            labels.add(member.name() + " — " + roleLabel(member.role()));
        }
        return labels;
    }

    private static String lateSummary(ReportSummary summary) {
        if (summary.lateStages() == 0 && summary.lateTasks() == 0) {
            return "Nenhum atraso registrado no período.";
        }
        return summary.lateStages() + " etapa(s) e " + summary.lateTasks() + " tarefa(s) em atraso.";
    }

    private static String address(ConstructionProject project) {
        StringBuilder text = new StringBuilder();
        append(text, project.getStreet(), "");
        if (project.getNumber() != null && !project.getNumber().isBlank()) {
            text.append(text.length() > 0 ? ", " : "").append(project.getNumber());
        }
        append(text, project.getComplement(), " - ");
        append(text, project.getCity(), " - ");
        if (project.getState() != null && !project.getState().isBlank()) {
            text.append(text.length() > 0 ? "/" : "").append(project.getState());
        }
        append(text, project.getCep(), " - CEP ");
        return text.length() == 0 ? EMPTY : text.toString();
    }

    private static void append(StringBuilder text, String value, String separator) {
        if (value != null && !value.isBlank()) {
            text.append(text.length() > 0 ? separator : "").append(value);
        }
    }

    private static String plannedWindow(ConstructionProject project) {
        LocalDate start = project.getPlannedStartDate();
        LocalDate end = project.getPlannedEndDate();
        if (start == null && end == null) {
            return EMPTY;
        }
        return (start == null ? EMPTY : start.format(DATE)) + " a " + (end == null ? EMPTY : end.format(DATE));
    }

    private static String area(BigDecimal value) {
        return value == null ? EMPTY : value.stripTrailingZeros().toPlainString() + " m²";
    }

    private static String nullToDash(String value) {
        return value == null || value.isBlank() ? EMPTY : value;
    }

    /** Rótulos em português; status desconhecido volta como veio, em vez de sumir. */
    private static String statusLabel(String status) {
        if (status == null) {
            return EMPTY;
        }
        return switch (status) {
            case "PLANNING" -> "Planejamento";
            case "PLANNED" -> "Planejada";
            case "IN_PROGRESS" -> "Em andamento";
            case "PAUSED" -> "Pausada";
            case "BLOCKED" -> "Bloqueada";
            case "COMPLETED" -> "Concluída";
            case "CANCELLED" -> "Cancelada";
            case "DONE" -> "Concluída";
            case "TODO" -> "A fazer";
            default -> status;
        };
    }

    private static String roleLabel(String role) {
        if (role == null) {
            return EMPTY;
        }
        return switch (role) {
            case "OWNER" -> "Responsável";
            case "ENGINEER" -> "Engenheiro(a)";
            case "ARCHITECT" -> "Arquiteto(a)";
            case "FOREMAN" -> "Mestre de obras";
            case "USER" -> "Cliente";
            default -> role;
        };
    }
}
