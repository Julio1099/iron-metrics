package com.ironmetrics.training.api;

import com.ironmetrics.training.domain.WorkoutSession;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record WorkoutSessionResponse(
        UUID id,
        LocalDate sessionDate,
        String title,
        BigDecimal bodyWeightKg,
        List<WorkoutSetResponse> sets
) {

    public static WorkoutSessionResponse from(WorkoutSession workoutSession) {
        return new WorkoutSessionResponse(
                workoutSession.getId(),
                workoutSession.getSessionDate(),
                workoutSession.getTitle(),
                workoutSession.getBodyWeightKg(),
                workoutSession.getSets().stream()
                        .map(WorkoutSetResponse::from)
                        .toList()
        );
    }
}
