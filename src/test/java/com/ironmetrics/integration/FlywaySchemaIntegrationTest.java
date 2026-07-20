package com.ironmetrics.integration;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class FlywaySchemaIntegrationTest extends PostgresIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registerPostgresProperties(registry);
    }

    @Test
    void shouldCreateTransactionalAndAnalyticsSchemas() {
        assertThat(schemaExists("public")).isTrue();
        assertThat(schemaExists("analytics")).isTrue();
    }

    @Test
    void shouldRunOnlyCoreMigrationsForTestProfile() {
        Integer appliedMigrations = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM public.flyway_schema_history WHERE success = true AND type = 'SQL'",
                Integer.class
        );

        assertThat(appliedMigrations).isEqualTo(2);
    }

    @Test
    void shouldCreateCoreTransactionalTablesInPublicSchema() {
        assertThat(tableExists("public", "users")).isTrue();
        assertThat(tableExists("public", "exercises")).isTrue();
        assertThat(tableExists("public", "workout_sessions")).isTrue();
        assertThat(tableExists("public", "workout_sets")).isTrue();
        assertThat(tableExists("public", "food_items")).isTrue();
        assertThat(tableExists("public", "nutrition_logs")).isTrue();
    }

    private Boolean schemaExists(String schemaName) {
        return jdbcTemplate.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM information_schema.schemata WHERE schema_name = ?)",
                Boolean.class,
                schemaName
        );
    }

    private Boolean tableExists(String schemaName, String tableName) {
        return jdbcTemplate.queryForObject(
                """
                SELECT EXISTS (
                    SELECT 1
                    FROM information_schema.tables
                    WHERE table_schema = ?
                    AND table_name = ?
                )
                """,
                Boolean.class,
                schemaName,
                tableName
        );
    }
}
