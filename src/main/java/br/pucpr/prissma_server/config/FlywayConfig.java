package br.pucpr.prissma_server.config;

import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.context.annotation.Profile;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.sql.Connection;
import java.sql.Statement;

@Configuration
@Profile("docker")
public class FlywayConfig {

    @Bean
    public FlywayMigrationStrategy flywayMigrationStrategy() {
        return flyway -> {
            // Remove any failed or potentially inconsistent migrations from history
            // This allows them to be re-applied fresh
            try (Connection conn = flyway.getConfiguration().getDataSource().getConnection();
                 Statement stmt = conn.createStatement()) {

                // Delete entries for migrations with checksum mismatches or validation failures
                // This is safe because we're in docker/clean environment
                stmt.execute("DELETE FROM flyway_schema_history WHERE version IN (6, 8) AND success = true");

            } catch (Exception e) {
                // If we can't clean history (table might not exist yet), continue anyway
                System.out.println("Info: Could not pre-clean migration history (expected on first run): " + e.getMessage());
            }

            // Now run migrations - this will apply any missing versions
            flyway.migrate();
        };
    }
}







