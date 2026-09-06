package br.pucpr.prissma_server.report;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Geometria dos gráficos — números, não markup.
 *
 * O SVG em si é escrito no template, e não montado aqui como string inserida via
 * th:utext: markup vindo de string passa pelo parser do Thymeleaf e volta
 * alterado, o que quebrava o documento com "The element type svg must be
 * terminated by the matching end-tag". Com o SVG no template, o markup é fixo e
 * só os atributos numéricos variam.
 *
 * Todo valor sai como String formatada em Locale.US de propósito: um double
 * entregue ao template seria formatado em pt-BR e viraria "52,00", que é
 * atributo SVG inválido.
 */
final class ReportCharts {

    private static final String COLOR_LATE = "#B42318";
    private static final String COLOR_AHEAD = "#067647";

    /** Coordenadas do viewBox do gráfico de desvio (o template usa as mesmas). */
    static final int DEVIATION_WIDTH = 520;
    static final int DEVIATION_HEIGHT = 120;
    static final int DEVIATION_BASELINE = DEVIATION_HEIGHT / 2;

    private static final double DONUT_RADIUS = 52.0;

    private ReportCharts() {
    }

    /**
     * Anel de progresso via stroke-dasharray: o traço cobre a fração concluída da
     * circunferência e o resto fica em branco. Sem trigonometria e sem o caso de
     * borda do large-arc-flag em exatamente 50%, que um <path> de arco teria.
     */
    static ProjectReportView.Donut donut(int done, int total) {
        double circumference = 2 * Math.PI * DONUT_RADIUS;
        double fraction = total <= 0 ? 0.0 : Math.min(1.0, (double) done / total);
        double filled = circumference * fraction;

        return new ProjectReportView.Donut(
                filled > 0,
                fmt(DONUT_RADIUS),
                fmt(filled) + " " + fmt(circumference - filled));
    }

    /**
     * Barras em torno da linha de base zero: atraso desce, adiantamento sobe.
     * Etapa sem desvio calculável ocupa a coluna, mas não gera barra.
     */
    static List<ProjectReportView.Bar> deviationBars(List<ReportSummary.StageLine> stages) {
        List<ProjectReportView.Bar> bars = new ArrayList<>();
        if (stages.isEmpty()) {
            return bars;
        }

        long maxAbs = 1;
        for (ReportSummary.StageLine stage : stages) {
            if (stage.deviationDays() != null) {
                maxAbs = Math.max(maxAbs, Math.abs(stage.deviationDays()));
            }
        }

        double slot = (double) DEVIATION_WIDTH / stages.size();
        double barWidth = Math.min(46.0, slot * 0.55);
        double maxBar = DEVIATION_BASELINE - 8.0;

        for (int i = 0; i < stages.size(); i++) {
            Long deviation = stages.get(i).deviationDays();
            if (deviation == null || deviation == 0) {
                continue;
            }
            double barHeight = Math.abs(deviation) / (double) maxAbs * maxBar;
            double centerX = slot * i + slot / 2.0;

            bars.add(new ProjectReportView.Bar(
                    fmt(centerX - barWidth / 2.0),
                    fmt(deviation > 0 ? DEVIATION_BASELINE : DEVIATION_BASELINE - barHeight),
                    fmt(barWidth),
                    fmt(barHeight),
                    deviation > 0 ? COLOR_LATE : COLOR_AHEAD));
        }
        return bars;
    }

    private static String fmt(double value) {
        return String.format(Locale.US, "%.2f", value);
    }
}
