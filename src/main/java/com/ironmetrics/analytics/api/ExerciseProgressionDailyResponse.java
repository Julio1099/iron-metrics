package com.ironmetrics.analytics.api;

import com.ironmetrics.analytics.domain.ExerciseProgressionDaily;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record ExerciseProgressionDailyResponse(
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

    static ExerciseProgressionDailyResponse from(ExerciseProgressionDaily readModel) {
        return new ExerciseProgressionDailyResponse(
                readModel.exerciseId(),
                readModel.exerciseName(),
                readModel.primaryMuscleGroup(),
                readModel.movementPattern(),
                readModel.trainingDate(),
                readModel.performedSets(),
                readModel.totalRepetitions(),
                readModel.totalVolumeKg(),
                readModel.topLoadKg(),
                readModel.maxEstimatedOneRepMaxKg(),
                readModel.averageRpe()
        );
    }
}
