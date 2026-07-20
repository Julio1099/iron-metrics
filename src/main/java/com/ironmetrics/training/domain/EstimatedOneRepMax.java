package com.ironmetrics.training.domain;

import java.math.BigDecimal;

public record EstimatedOneRepMax(
        BigDecimal value,
        BigDecimal repsInReserve,
        BigDecimal effectiveRepetitions
) {
}
