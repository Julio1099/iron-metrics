package com.ironmetrics.analytics.application;

import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AnalyticsRefreshService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AnalyticsRefreshService.class);

    private final JdbcTemplate jdbcTemplate;

    public AnalyticsRefreshService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional
    public AnalyticsRefreshResult refresh() {
        LOGGER.info("Starting analytics read model refresh.");

        AnalyticsRefreshResult result = Objects.requireNonNull(jdbcTemplate.queryForObject(
                """
                SELECT
                    training_volume_rows,
                    exercise_progression_rows,
                    body_weight_rows
                FROM analytics.refresh_body_recomposition_read_models()
                """,
                (resultSet, rowNumber) -> new AnalyticsRefreshResult(
                        resultSet.getInt("training_volume_rows"),
                        resultSet.getInt("exercise_progression_rows"),
                        resultSet.getInt("body_weight_rows")
                )
        ));

        LOGGER.info(
                "Completed analytics read model refresh: trainingVolumeRows={}, exerciseProgressionRows={}, bodyWeightRows={}.",
                result.trainingVolumeRows(),
                result.exerciseProgressionRows(),
                result.bodyWeightRows()
        );

        return result;
    }
}
