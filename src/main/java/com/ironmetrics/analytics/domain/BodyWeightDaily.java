package com.ironmetrics.analytics.domain;

import java.math.BigDecimal;
import java.time.LocalDate;

public record BodyWeightDaily(
        LocalDate measurementDate,
        BigDecimal bodyWeightKg,
        int sourceSessions
) {
}
