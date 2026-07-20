package com.ironmetrics.training.api;

import com.ironmetrics.training.domain.WorkoutSet;
import java.math.BigDecimal;
import java.util.UUID;

public record WorkoutSetResponse(
        UUID id,
        UUID exerciseId,
        String exerciseName,
        int setOrder,
        BigDecimal loadKg,
        int repetitions,
        BigDecimal rpe,
        BigDecimal estimatedOneRepMaxKg
) {

    public static WorkoutSetResponse from(WorkoutSet workoutSet) {
        return new WorkoutSetResponse(
                workoutSet.getId(),
                workoutSet.getExercise().getId(),
                workoutSet.getExercise().getName(),
                workoutSet.getSetOrder(),
                workoutSet.getLoadKg(),
                workoutSet.getRepetitions(),
                workoutSet.getRpe(),
                workoutSet.getEstimatedOneRepMaxKg()
        );
    }
}
