package com.ironmetrics.analytics.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record ExerciseProgressionDaily(
        UUID exerciseId,
        String exerciseName,
        String primaryMuscleGroup,
        String movementPattern,
        LocalDate trainingDate,
        int performedSets,
        int totalRepetitions,
        BigDecimal totalVolumeKg,
        BigDecimal topLoadKg,
        BigDecimal maxEstimatedOneRepMaxKg,
        BigDecimal averageRpe
) {
}
