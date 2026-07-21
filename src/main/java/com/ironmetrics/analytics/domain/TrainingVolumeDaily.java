package com.ironmetrics.analytics.domain;

import java.math.BigDecimal;
import java.time.LocalDate;

public record TrainingVolumeDaily(
        LocalDate trainingDate,
        int totalSets,
        int totalRepetitions,
        BigDecimal totalVolumeKg,
        BigDecimal averageRpe,
        BigDecimal maxEstimatedOneRepMaxKg
) {
}
