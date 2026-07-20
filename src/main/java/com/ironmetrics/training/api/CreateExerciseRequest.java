package com.ironmetrics.training.api;

import com.ironmetrics.training.domain.MechanicsType;
import com.ironmetrics.training.domain.MovementPattern;
import com.ironmetrics.training.domain.MuscleGroup;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateExerciseRequest(
        @NotBlank
        @Size(max = 120)
        String name,

        @NotNull
        MuscleGroup primaryMuscleGroup,

        @NotNull
        MovementPattern movementPattern,

        @NotNull
        MechanicsType mechanicsType
) {
}
