package br.pucpr.prissma_server.report;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Geometria dos gráficos. O ponto sensível aqui é o FORMATO dos números: o SVG
 * exige ponto decimal, e um valor formatado em pt-BR ("52,00") passaria batido na
 * compilação e só quebraria o desenho em runtime.
 */
@DisplayName("ReportCharts Tests")
class ReportChartsTest {

    private ReportSummary.StageLine line(String name, Long deviation) {
        return new ReportSummary.StageLine(name, "DONE",
                LocalDate.of(2026, 6, 10), LocalDate.of(2026, 6, 12),
                deviation, deviation != null && deviation > 0);
    }

    private void assertSvgNumber(String value) {
        assertNotNull(value);
        assertFalse(value.contains(","), "número em formato pt-BR quebra o SVG: " + value);
        assertTrue(value.matches("-?\\d+\\.\\d{2}"), "esperava número simples com ponto: " + value);
    }

    @Test
    @DisplayName("donut parcial preenche a fração concluída da circunferência")
    void partialDonut() {
        ProjectReportView.Donut donut = ReportCharts.donut(1, 4);

        assertTrue(donut.hasProgress());
        assertSvgNumber(donut.radius());

        String[] dash = donut.dashArray().split(" ");
        assertEquals(2, dash.length, "stroke-dasharray tem traço e lacuna");
        assertSvgNumber(dash[0]);
        assertSvgNumber(dash[1]);

        double circumference = 2 * Math.PI * 52.0;
        assertEquals(circumference / 4, Double.parseDouble(dash[0]), 0.01, "1 de 4 = um quarto do anel");
    }

    @Test
    @DisplayName("obra sem etapas não divide por zero e não desenha progresso")
    void emptyDonut() {
        ProjectReportView.Donut donut = ReportCharts.donut(0, 0);

        assertFalse(donut.hasProgress());
        assertEquals(0.0, Double.parseDouble(donut.dashArray().split(" ")[0]), 0.001);
    }

    @Test
    @DisplayName("donut completo não passa de 100%")
    void fullDonut() {
        ProjectReportView.Donut donut = ReportCharts.donut(7, 5);

        double circumference = 2 * Math.PI * 52.0;
        assertEquals(circumference, Double.parseDouble(donut.dashArray().split(" ")[0]), 0.01);
    }

    @Test
    @DisplayName("atraso desce da linha de base e adiantamento sobe")
    void deviationDirection() {
        List<ProjectReportView.Bar> bars = ReportCharts.deviationBars(List.of(
                line("Fundação", 6L),
                line("Alvenaria", -6L)));

        assertEquals(2, bars.size());

        double baseline = ReportCharts.DEVIATION_BASELINE;
        assertEquals(baseline, Double.parseDouble(bars.get(0).y()), 0.01, "atraso começa na linha e desce");
        assertTrue(Double.parseDouble(bars.get(1).y()) < baseline, "adiantamento sobe acima da linha");
        assertNotEquals(bars.get(0).color(), bars.get(1).color(), "cores distintas por sinal");
    }

    @Test
    @DisplayName("etapa sem desvio calculável não gera barra")
    void skipsStagesWithoutDeviation() {
        List<ProjectReportView.Bar> bars = ReportCharts.deviationBars(List.of(
                line("Fundação", 6L),
                line("Sem datas", null),
                line("No prazo", 0L)));

        assertEquals(1, bars.size());
    }

    @Test
    @DisplayName("sem etapas, não há barra alguma")
    void emptyDeviation() {
        assertTrue(ReportCharts.deviationBars(List.of()).isEmpty());
    }

    @Test
    @DisplayName("todas as coordenadas saem em formato aceito pelo SVG")
    void allCoordinatesAreSvgSafe() {
        List<ProjectReportView.Bar> bars = ReportCharts.deviationBars(List.of(
                line("A", 3L), line("B", -7L), line("C", 12L)));

        for (ProjectReportView.Bar bar : bars) {
            assertSvgNumber(bar.x());
            assertSvgNumber(bar.y());
            assertSvgNumber(bar.width());
            assertSvgNumber(bar.height());
            assertTrue(bar.color().startsWith("#"), "cor deve ser hexadecimal: " + bar.color());
        }
    }
}
