package br.pucpr.prissma_server.report;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * O template do relatório precisa ser XML BEM FORMADO — o openhtmltopdf o
 * parseia com Xerces, não com um parser de HTML.
 *
 * Este teste existe por causa de um bug real e caro de achar: um comentário de
 * CSS dentro do bloco style citava um nome de tag entre sinais de menor/maior.
 * Em HTML isso é inofensivo, porque style é raw text; em XML não é, e aquele
 * nome virou um elemento aberto de verdade. O renderer então acusava o SVG como
 * não terminado, mesmo com o SVG perfeitamente correto — a mensagem apontava
 * para bem longe da causa.
 *
 * Roda em milissegundos e sem Docker, então pega esse tipo de erro muito antes
 * do teste de integração que gera o PDF.
 */
@DisplayName("Template do relatório")
class ReportTemplateXmlTest {

    private static final String TEMPLATE = "/templates/reports/project-report.html";

    @Test
    @DisplayName("é XML bem formado, como o renderer exige")
    void templateIsWellFormedXml() throws Exception {
        try (InputStream in = getClass().getResourceAsStream(TEMPLATE)) {
            assertNotNull(in, "template não encontrado no classpath: " + TEMPLATE);

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);   // o template declara o namespace th
            factory.setValidating(false);

            try {
                factory.newDocumentBuilder().parse(in);
            } catch (Exception ex) {
                fail("O template não é XML válido e vai derrubar a geração do PDF. "
                        + "Suspeite de nome de tag citado dentro de style ou de comentário, "
                        + "de tag sem fechamento, ou de dois hifens seguidos num comentário.\n"
                        + ex.getMessage());
            }
        }
    }
}
