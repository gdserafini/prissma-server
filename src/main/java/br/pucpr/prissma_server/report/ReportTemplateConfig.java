package br.pucpr.prissma_server.report;

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.thymeleaf.spring6.templateresolver.SpringResourceTemplateResolver;
import org.thymeleaf.templatemode.TemplateMode;

import java.util.Set;

/**
 * Resolve os templates de relatório em modo XML, e não no modo HTML padrão.
 *
 * Por quê: o openhtmltopdf parseia o documento com Xerces e exige XML bem
 * formado. No modo HTML o Thymeleaf aplica as regras de HTML5 ao serializar e
 * NÃO devolve o markup como escrito — no caso do &lt;svg&gt; ele descarta a tag de
 * fechamento, e a renderização morre com "The element type svg must be
 * terminated by the matching end-tag". No modo XML o parser não tem regra
 * nenhuma de elemento void ou de auto-fechamento: o que está escrito é o que sai.
 *
 * O escopo é limitado a "reports/*" por resolvablePatterns, em vez de trocar o
 * spring.thymeleaf.mode global: qualquer template futuro do projeto continua em
 * HTML, com o comportamento que se espera dele.
 *
 * Consequência para quem editar o template: ele precisa ser XML válido. Toda tag
 * fechada (inclusive &lt;meta/&gt; e &lt;div&gt;&lt;/div&gt; vazias), atributos sempre entre
 * aspas e nenhuma entidade nomeada de HTML (nbsp, mdash) — só numéricas.
 */
@Configuration
public class ReportTemplateConfig {

    @Bean
    public SpringResourceTemplateResolver reportTemplateResolver(ApplicationContext applicationContext) {
        SpringResourceTemplateResolver resolver = new SpringResourceTemplateResolver();
        resolver.setApplicationContext(applicationContext);
        resolver.setPrefix("classpath:/templates/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode(TemplateMode.XML);
        resolver.setCharacterEncoding("UTF-8");
        // Só os templates de relatório passam por aqui; o resto cai no resolver
        // padrão do Spring Boot, que continua em modo HTML.
        resolver.setResolvablePatterns(Set.of("reports/*"));
        // Ordem menor = maior prioridade. O resolver padrão do Boot não define
        // ordem, então fica atrás deste.
        resolver.setOrder(1);
        resolver.setCacheable(true);
        return resolver;
    }
}
