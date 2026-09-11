package br.pucpr.prissma_server.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Execução assíncrona.
 *
 * A geração de prévia por IA leva de 30 a 60s por chamada. Um pool próprio e
 * pequeno mantém isso longe do executor padrão do Spring: se a OpenAI ficar
 * lenta, quem espera é a fila de prévias, não o resto da aplicação.
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    public static final String PREVIEW_EXECUTOR = "genaiPreviewExecutor";

    @Bean(PREVIEW_EXECUTOR)
    public Executor genaiPreviewExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(20);
        executor.setThreadNamePrefix("genai-preview-");
        // Fila cheia: quem pediu espera na própria thread em vez de perder o
        // pedido. Uma prévia perdida é dinheiro gasto na OpenAI sem resultado.
        executor.setRejectedExecutionHandler(new java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }
}
