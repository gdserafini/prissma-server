package br.pucpr.prissma_server;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Postgres real (Testcontainers) para os testes de integração.
 *
 * O H2 em MODE=PostgreSQL nunca conseguiu rodar as migrations reais do projeto
 * (V2 falha em TIMESTAMPTZ; V6/V11/V12 fazem DROP CONSTRAINT com nomes gerados
 * pelo Postgres). Com um Postgres de verdade, o que a suíte valida é exatamente
 * o que roda em produção — incluindo as migrations de workspace (V13+), que
 * usam índices únicos parciais e backfill com DISTINCT ON.
 *
 * O container é ESTÁTICO e compartilhado: sobe uma única vez para a suíte
 * inteira (não um Postgres por classe @SpringBootTest). O Testcontainers
 * derruba tudo no fim via Ryuk. Requer Docker em execução.
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfig {

    private static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:17-alpine");

    static {
        POSTGRES.start();
    }

    @Bean
    @ServiceConnection
    PostgreSQLContainer<?> postgresContainer() {
        return POSTGRES;
    }
}
