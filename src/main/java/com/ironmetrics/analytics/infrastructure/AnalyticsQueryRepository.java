package com.ironmetrics.analytics.infrastructure;

import com.ironmetrics.analytics.domain.BodyWeightDaily;
import com.ironmetrics.analytics.domain.ExerciseProgressionDaily;
import com.ironmetrics.analytics.domain.TrainingVolumeDaily;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class AnalyticsQueryRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public AnalyticsQueryRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<TrainingVolumeDaily> findTrainingVolumeDaily(UUID userId, LocalDate from, LocalDate to) {
        return jdbcTemplate.query(
                """
                SELECT
                    training_date,
                    total_sets,
                    total_repetitions,
                    total_volume_kg,
                    average_rpe,
                    max_estimated_one_rep_max_kg
                FROM analytics.training_volume_daily
                WHERE user_id = :userId
                AND (:fromDate IS NULL OR training_date >= :fromDate)
                AND (:toDate IS NULL OR training_date <= :toDate)
                ORDER BY training_date
                """,
                parameters(userId, from, to),
                this::mapTrainingVolumeDaily
        );
    }

    public List<ExerciseProgressionDaily> findExerciseProgressionDaily(UUID userId, LocalDate from, LocalDate to) {
        return jdbcTemplate.query(
                """
                SELECT
                    exercise_id,
                    exercise_name,
                    primary_muscle_group,
                    movement_pattern,
                    training_date,
                    performed_sets,
                    total_repetitions,
                    total_volume_kg,
                    top_load_kg,
                    max_estimated_one_rep_max_kg,
                    average_rpe
                FROM analytics.exercise_progression_daily
                WHERE user_id = :userId
                AND (:fromDate IS NULL OR training_date >= :fromDate)
                AND (:toDate IS NULL OR training_date <= :toDate)
                ORDER BY training_date, exercise_name
                """,
                parameters(userId, from, to),
                this::mapExerciseProgressionDaily
        );
    }

    public List<BodyWeightDaily> findBodyWeightDaily(UUID userId, LocalDate from, LocalDate to) {
        return jdbcTemplate.query(
                """
                SELECT
                    measurement_date,
                    body_weight_kg,
                    source_sessions
                FROM analytics.body_weight_daily
                WHERE user_id = :userId
                AND (:fromDate IS NULL OR measurement_date >= :fromDate)
                AND (:toDate IS NULL OR measurement_date <= :toDate)
                ORDER BY measurement_date
                """,
                parameters(userId, from, to),
                this::mapBodyWeightDaily
        );
    }

    private MapSqlParameterSource parameters(UUID userId, LocalDate from, LocalDate to) {
        return new MapSqlParameterSource()
                .addValue("userId", userId)
                .addValue("fromDate", from, Types.DATE)
                .addValue("toDate", to, Types.DATE);
    }

    private TrainingVolumeDaily mapTrainingVolumeDaily(ResultSet resultSet, int rowNumber) throws SQLException {
        return new TrainingVolumeDaily(
                resultSet.getDate("training_date").toLocalDate(),
                resultSet.getInt("total_sets"),
                resultSet.getInt("total_repetitions"),
                resultSet.getBigDecimal("total_volume_kg"),
                resultSet.getBigDecimal("average_rpe"),
                resultSet.getBigDecimal("max_estimated_one_rep_max_kg")
        );
    }

    private ExerciseProgressionDaily mapExerciseProgressionDaily(ResultSet resultSet, int rowNumber) throws SQLException {
        return new ExerciseProgressionDaily(
                resultSet.getObject("exercise_id", UUID.class),
                resultSet.getString("exercise_name"),
                resultSet.getString("primary_muscle_group"),
                resultSet.getString("movement_pattern"),
                resultSet.getDate("training_date").toLocalDate(),
                resultSet.getInt("performed_sets"),
                resultSet.getInt("total_repetitions"),
                resultSet.getBigDecimal("total_volume_kg"),
                resultSet.getBigDecimal("top_load_kg"),
                resultSet.getBigDecimal("max_estimated_one_rep_max_kg"),
                resultSet.getBigDecimal("average_rpe")
        );
    }

    private BodyWeightDaily mapBodyWeightDaily(ResultSet resultSet, int rowNumber) throws SQLException {
        return new BodyWeightDaily(
                resultSet.getDate("measurement_date").toLocalDate(),
                resultSet.getBigDecimal("body_weight_kg"),
                resultSet.getInt("source_sessions")
        );
    }
}
