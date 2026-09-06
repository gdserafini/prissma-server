package br.pucpr.prissma_server.report;

import com.openhtmltopdf.outputdevice.helper.BaseRendererBuilder;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import com.openhtmltopdf.svgsupport.BatikSVGDrawer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.EnumSet;
import java.util.Locale;

/**
 * Renderiza o relatório: Thymeleaf -> XHTML -> OpenHTMLtoPDF -> bytes.
 */
@Component
public class ProjectReportPdfRenderer {

    private static final Logger LOG = LoggerFactory.getLogger(ProjectReportPdfRenderer.class);

    static final Locale PT_BR = Locale.of("pt", "BR");

    private static final String TEMPLATE = "reports/project-report";

    /**
     * Fonte opcional. Sem ela o renderer cai nas Standard 14 do PDF (Helvetica,
     * codificada em WinAnsi), que cobrem todos os acentos do português — por isso
     * o relatório funciona sem nenhum arquivo de fonte no repositório.
     *
     * Se os arquivos existirem em resources/fonts, são registrados e passam a ser
     * usados: aí a fonte vai EMBUTIDA no PDF, o que torna a aparência idêntica em
     * qualquer visualizador e libera caracteres fora do WinAnsi (que, sem
     * embutir, viram '#' silenciosamente).
     */
    private static final String FONT_FAMILY = "Report Sans";
    private static final String FONT_REGULAR = "/fonts/NotoSans-Regular.ttf";
    private static final String FONT_BOLD = "/fonts/NotoSans-Bold.ttf";

    private final SpringTemplateEngine templateEngine;

    public ProjectReportPdfRenderer(SpringTemplateEngine templateEngine) {
        this.templateEngine = templateEngine;
    }

    public byte[] render(ProjectReportView view) {
        Context context = new Context(PT_BR);
        context.setVariable("report", view);

        String xhtml = templateEngine.process(TEMPLATE, context);

        try (ByteArrayOutputStream out = new ByteArrayOutputStream(128 * 1024)) {
            PdfRendererBuilder builder = new PdfRendererBuilder();

            // Construtor padrão = SvgScriptMode.SECURE + SvgExternalResourceMode.SECURE:
            // sem scripting e sem buscar recurso externo a partir do SVG.
            builder.useSVGDrawer(new BatikSVGDrawer());

            registerFonts(builder);

            // baseUri nulo: todo o CSS está inline no <style> e não há recurso relativo.
            builder.withHtmlContent(xhtml, null);
            builder.toStream(out);
            builder.run();

            return out.toByteArray();
        } catch (IOException | RuntimeException ex) {
            // RuntimeException entra aqui de propósito: o renderer sinaliza XHTML
            // malformado com XRRuntimeException, não com IOException, e sem este
            // catch a mensagem crua da biblioteca vazava direto na resposta HTTP.
            // O stack trace vai para o log, onde serve para diagnóstico.
            LOG.error("Falha ao renderizar o relatório em PDF", ex);
            // O erro do renderer aponta linha/coluna do XHTML; sem o documento em
            // mãos esse número não diz nada. Ative o DEBUG deste pacote para vê-lo.
            LOG.debug("XHTML que falhou:\n{}", xhtml);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Não foi possível gerar o relatório em PDF");
        }
    }

    private void registerFonts(PdfRendererBuilder builder) {
        if (!fontExists(FONT_REGULAR)) {
            return;
        }
        // O EnumSet é o ponto crítico: os overloads de conveniência registram a
        // fonte só para DOCUMENT, e aí qualquer <text> dentro de SVG não renderiza.
        builder.useFont(() -> resource(FONT_REGULAR), FONT_FAMILY, 400,
                BaseRendererBuilder.FontStyle.NORMAL, true,
                EnumSet.of(BaseRendererBuilder.FSFontUseCase.DOCUMENT,
                        BaseRendererBuilder.FSFontUseCase.SVG));

        if (fontExists(FONT_BOLD)) {
            // Peso 700 precisa de arquivo próprio: o renderer não sintetiza negrito.
            builder.useFont(() -> resource(FONT_BOLD), FONT_FAMILY, 700,
                    BaseRendererBuilder.FontStyle.NORMAL, true,
                    EnumSet.of(BaseRendererBuilder.FSFontUseCase.DOCUMENT,
                            BaseRendererBuilder.FSFontUseCase.SVG));
        }
    }

    private boolean fontExists(String path) {
        return ProjectReportPdfRenderer.class.getResource(path) != null;
    }

    /**
     * Stream novo a cada chamada: o supplier é consultado uma vez por caso de uso
     * registrado, então um InputStream cacheado viria já consumido na segunda vez.
     */
    private InputStream resource(String path) {
        return ProjectReportPdfRenderer.class.getResourceAsStream(path);
    }
}
