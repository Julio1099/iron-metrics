package com.ironmetrics.training.api;

import com.ironmetrics.training.domain.Exercise;
import com.ironmetrics.training.domain.MechanicsType;
import com.ironmetrics.training.domain.MovementPattern;
import com.ironmetrics.training.domain.MuscleGroup;
import java.time.Instant;
import java.util.UUID;

public record ExerciseResponse(
        UUID id,
        String name,
        MuscleGroup primaryMuscleGroup,
        MovementPattern movementPattern,
        MechanicsType mechanicsType,
        Instant createdAt,
        Instant updatedAt
) {

    public static ExerciseResponse from(Exercise exercise) {
        return new ExerciseResponse(
                exercise.getId(),
                exercise.getName(),
                exercise.getPrimaryMuscleGroup(),
                exercise.getMovementPattern(),
                exercise.getMechanicsType(),
                exercise.getCreatedAt(),
                exercise.getUpdatedAt()
        );
    }
}
