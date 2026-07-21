package com.ironmetrics.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@ActiveProfiles("dev")
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "logging.level.org.hibernate.SQL=off",
                "spring.jpa.show-sql=false"
        }
)
class FlywayDevSeedIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("iron_metrics_dev_seed_test")
            .withUsername("iron_metrics")
            .withPassword("iron_metrics_dev_seed_password");

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @DynamicPropertySource
    static void registerPostgresProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Test
    void shouldLoadDevSeedOnlyWhenDevProfileIsActive() {
        Integer appliedMigrations = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM public.flyway_schema_history WHERE success = true AND type = 'SQL'",
                Integer.class
        );
        Integer demoUsers = countRows("users", "email = 'demo@ironmetrics.dev'");
        Integer demoExercises = countRows("exercises", "name IN ('Bench Press', 'Back Squat', 'Deadlift', 'Lat Pulldown')");
        Integer demoWorkoutSets = countRows("workout_sets", "workout_session_id = '00000000-0000-0000-0000-000000000301'");
        BigDecimal benchEstimatedOneRepMax = jdbcTemplate.queryForObject(
                """
                SELECT estimated_one_rep_max_kg
                FROM public.workout_sets
                WHERE id = '00000000-0000-0000-0000-000000000401'
                """,
                BigDecimal.class
        );
        String demoPasswordHash = jdbcTemplate.queryForObject(
                "SELECT password_hash FROM public.users WHERE email = 'demo@ironmetrics.dev'",
                String.class
        );

        Integer demoAnalyticsTrainingRows = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM analytics.training_volume_daily
                WHERE user_id = '00000000-0000-0000-0000-000000000101'
                AND training_date = '2026-07-20'
                """,
                Integer.class
        );

        assertThat(appliedMigrations).isEqualTo(4);
        assertThat(demoUsers).isEqualTo(1);
        assertThat(demoExercises).isEqualTo(4);
        assertThat(demoWorkoutSets).isEqualTo(4);
        assertThat(benchEstimatedOneRepMax).isEqualByComparingTo("120.00");
        assertThat(demoAnalyticsTrainingRows).isEqualTo(1);
        assertThat(passwordEncoder.matches("Password123!", demoPasswordHash)).isTrue();
    }

    private Integer countRows(String tableName, String condition) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM public." + tableName + " WHERE " + condition,
                Integer.class
        );
    }
}
