package com.ironmetrics.training.api;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.UUID;

public record CreateWorkoutSetRequest(
        @NotNull
        UUID exerciseId,

        @Min(1)
        int setOrder,

        @NotNull
        @DecimalMin(value = "0.00")
        @Digits(integer = 4, fraction = 2)
        BigDecimal loadKg,

        @Min(1)
        int repetitions,

        @NotNull
        @DecimalMin(value = "1.0")
        @DecimalMax(value = "10.0")
        @Digits(integer = 2, fraction = 1)
        BigDecimal rpe
) {
}
