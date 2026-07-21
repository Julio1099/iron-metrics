package com.ironmetrics.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.ironmetrics.analytics.application.AnalyticsRefreshResult;
import com.ironmetrics.analytics.application.AnalyticsRefreshService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;
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
class AnalyticsRefreshIntegrationTest extends PostgresIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private AnalyticsRefreshService analyticsRefreshService;

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registerPostgresProperties(registry);
    }

    @Test
    void shouldRefreshAnalyticsReadModelsFromTransactionalTablesIdempotently() {
        UUID userId = UUID.randomUUID();
        UUID benchPressId = UUID.randomUUID();
        UUID backSquatId = UUID.randomUUID();
        LocalDate trainingDate = LocalDate.of(2026, 7, 21);

        insertControlledDataset(userId, benchPressId, backSquatId, trainingDate);
        assertThat(countTrainingVolumeRows(userId)).isZero();

        AnalyticsRefreshResult firstRefresh = analyticsRefreshService.refresh();

        assertThat(firstRefresh.trainingVolumeRows()).isGreaterThanOrEqualTo(1);
        assertThat(firstRefresh.exerciseProgressionRows()).isGreaterThanOrEqualTo(2);
        assertThat(firstRefresh.bodyWeightRows()).isGreaterThanOrEqualTo(1);
        assertReadModels(userId, benchPressId, trainingDate);

        int trainingVolumeRowsAfterFirstRefresh = countTrainingVolumeRows(userId);
        int exerciseProgressionRowsAfterFirstRefresh = countExerciseProgressionRows(userId);
        int bodyWeightRowsAfterFirstRefresh = countBodyWeightRows(userId);

        analyticsRefreshService.refresh();

        assertThat(countTrainingVolumeRows(userId)).isEqualTo(trainingVolumeRowsAfterFirstRefresh);
        assertThat(countExerciseProgressionRows(userId)).isEqualTo(exerciseProgressionRowsAfterFirstRefresh);
        assertThat(countBodyWeightRows(userId)).isEqualTo(bodyWeightRowsAfterFirstRefresh);
        assertReadModels(userId, benchPressId, trainingDate);
    }

    private void insertControlledDataset(UUID userId, UUID benchPressId, UUID backSquatId, LocalDate trainingDate) {
        UUID sessionId = UUID.randomUUID();
        String suffix = UUID.randomUUID().toString();

        jdbcTemplate.update(
                """
                INSERT INTO public.users (id, email, display_name, password_hash)
                VALUES (?, ?, 'Analytics Tester', 'not-used-in-this-test')
                """,
                userId,
                "analytics-" + suffix + "@ironmetrics.test"
        );
        jdbcTemplate.update(
                """
                INSERT INTO public.exercises (id, name, primary_muscle_group, movement_pattern, mechanics_type)
                VALUES
                    (?, ?, 'CHEST', 'PUSH', 'COMPOUND'),
                    (?, ?, 'QUADRICEPS', 'SQUAT', 'COMPOUND')
                """,
                benchPressId,
                "Analytics Bench Press " + suffix,
                backSquatId,
                "Analytics Back Squat " + suffix
        );
        jdbcTemplate.update(
                """
                INSERT INTO public.workout_sessions (id, user_id, session_date, title, body_weight_kg)
                VALUES (?, ?, ?, 'Analytics Strength Day', 82.40)
                """,
                sessionId,
                userId,
                trainingDate
        );
        jdbcTemplate.update(
                """
                INSERT INTO public.workout_sets (
                    id,
                    workout_session_id,
                    exercise_id,
                    set_order,
                    load_kg,
                    repetitions,
                    rpe,
                    estimated_one_rep_max_kg
                )
                VALUES
                    (?, ?, ?, 1, 100.00, 5, 9.0, 120.00),
                    (?, ?, ?, 2, 105.00, 4, 9.5, 122.50),
                    (?, ?, ?, 3, 140.00, 5, 8.5, 170.33)
                """,
                UUID.randomUUID(),
                sessionId,
                benchPressId,
                UUID.randomUUID(),
                sessionId,
                benchPressId,
                UUID.randomUUID(),
                sessionId,
                backSquatId
        );
    }

    private void assertReadModels(UUID userId, UUID benchPressId, LocalDate trainingDate) {
        Map<String, Object> volume = jdbcTemplate.queryForMap(
                """
                SELECT total_sets, total_repetitions, total_volume_kg, average_rpe, max_estimated_one_rep_max_kg
                FROM analytics.training_volume_daily
                WHERE user_id = ?
                AND training_date = ?
                """,
                userId,
                trainingDate
        );
        assertThat(volume.get("total_sets")).isEqualTo(3);
        assertThat(volume.get("total_repetitions")).isEqualTo(14);
        assertThat(decimal(volume.get("total_volume_kg"))).isEqualByComparingTo("1620.00");
        assertThat(decimal(volume.get("average_rpe"))).isEqualByComparingTo("9.00");
        assertThat(decimal(volume.get("max_estimated_one_rep_max_kg"))).isEqualByComparingTo("170.33");

        Map<String, Object> benchProgression = jdbcTemplate.queryForMap(
                """
                SELECT performed_sets, total_repetitions, total_volume_kg, top_load_kg, max_estimated_one_rep_max_kg
                FROM analytics.exercise_progression_daily
                WHERE user_id = ?
                AND exercise_id = ?
                AND training_date = ?
                """,
                userId,
                benchPressId,
                trainingDate
        );
        assertThat(benchProgression.get("performed_sets")).isEqualTo(2);
        assertThat(benchProgression.get("total_repetitions")).isEqualTo(9);
        assertThat(decimal(benchProgression.get("total_volume_kg"))).isEqualByComparingTo("920.00");
        assertThat(decimal(benchProgression.get("top_load_kg"))).isEqualByComparingTo("105.00");
        assertThat(decimal(benchProgression.get("max_estimated_one_rep_max_kg"))).isEqualByComparingTo("122.50");

        Map<String, Object> bodyWeight = jdbcTemplate.queryForMap(
                """
                SELECT body_weight_kg, source_sessions
                FROM analytics.body_weight_daily
                WHERE user_id = ?
                AND measurement_date = ?
                """,
                userId,
                trainingDate
        );
        assertThat(decimal(bodyWeight.get("body_weight_kg"))).isEqualByComparingTo("82.40");
        assertThat(bodyWeight.get("source_sessions")).isEqualTo(1);
    }

    private int countTrainingVolumeRows(UUID userId) {
        return countRows("analytics.training_volume_daily", userId);
    }

    private int countExerciseProgressionRows(UUID userId) {
        return countRows("analytics.exercise_progression_daily", userId);
    }

    private int countBodyWeightRows(UUID userId) {
        return countRows("analytics.body_weight_daily", userId);
    }

    private int countRows(String tableName, UUID userId) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM " + tableName + " WHERE user_id = ?",
                Integer.class,
                userId
        );
    }

    private BigDecimal decimal(Object value) {
        return new BigDecimal(value.toString());
    }
}
