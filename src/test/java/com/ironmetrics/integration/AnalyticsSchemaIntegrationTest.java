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
class AnalyticsSchemaIntegrationTest extends PostgresIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registerPostgresProperties(registry);
    }

    @Test
    void shouldCreateAnalyticsReadModelsAndRefreshFunction() {
        assertThat(tableExists("analytics", "training_volume_daily")).isTrue();
        assertThat(tableExists("analytics", "exercise_progression_daily")).isTrue();
        assertThat(tableExists("analytics", "body_weight_daily")).isTrue();
        assertThat(functionExists("analytics", "refresh_body_recomposition_read_models")).isTrue();
    }

    @Test
    void shouldCreateAnalyticsReaderRoleWithReadOnlyPrivileges() {
        assertThat(roleExists("iron_metrics_analytics_reader")).isTrue();
        assertThat(schemaPrivilege("iron_metrics_analytics_reader", "analytics", "USAGE")).isTrue();
        assertThat(tablePrivilege("iron_metrics_analytics_reader", "analytics.training_volume_daily", "SELECT")).isTrue();
        assertThat(tablePrivilege("iron_metrics_analytics_reader", "analytics.training_volume_daily", "INSERT")).isFalse();
        assertThat(tablePrivilege("iron_metrics_analytics_reader", "public.users", "SELECT")).isFalse();
        assertThat(functionPrivilege(
                "iron_metrics_analytics_reader",
                "analytics.refresh_body_recomposition_read_models()",
                "EXECUTE"
        )).isFalse();
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

    private Boolean functionExists(String schemaName, String functionName) {
        return jdbcTemplate.queryForObject(
                """
                SELECT EXISTS (
                    SELECT 1
                    FROM information_schema.routines
                    WHERE routine_schema = ?
                    AND routine_name = ?
                )
                """,
                Boolean.class,
                schemaName,
                functionName
        );
    }

    private Boolean roleExists(String roleName) {
        return jdbcTemplate.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = ?)",
                Boolean.class,
                roleName
        );
    }

    private Boolean schemaPrivilege(String roleName, String schemaName, String privilege) {
        return jdbcTemplate.queryForObject(
                "SELECT has_schema_privilege(?, ?, ?)",
                Boolean.class,
                roleName,
                schemaName,
                privilege
        );
    }

    private Boolean tablePrivilege(String roleName, String tableName, String privilege) {
        return jdbcTemplate.queryForObject(
                "SELECT has_table_privilege(?, ?, ?)",
                Boolean.class,
                roleName,
                tableName,
                privilege
        );
    }

    private Boolean functionPrivilege(String roleName, String functionName, String privilege) {
        return jdbcTemplate.queryForObject(
                "SELECT has_function_privilege(?, ?, ?)",
                Boolean.class,
                roleName,
                functionName,
                privilege
        );
    }
}
