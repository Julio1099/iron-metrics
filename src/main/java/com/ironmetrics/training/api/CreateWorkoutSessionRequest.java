package com.ironmetrics.training.api;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateWorkoutSessionRequest(
        @NotBlank
        @Size(max = 120)
        String title,

        @NotNull
        LocalDate sessionDate,

        @DecimalMin(value = "0.01")
        @Digits(integer = 3, fraction = 2)
        BigDecimal bodyWeightKg
) {
}
