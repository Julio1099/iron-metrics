package com.ironmetrics.training.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class EstimatedOneRepMaxCalculator {

    private static final BigDecimal MINIMUM_RPE_FOR_ESTIMATION = new BigDecimal("7.0");
    private static final BigDecimal MAX_EFFECTIVE_REPETITIONS = new BigDecimal("12.0");
    private static final BigDecimal TEN = new BigDecimal("10.0");
    private static final BigDecimal EPLEY_DIVISOR = new BigDecimal("30.0");

    public Optional<EstimatedOneRepMax> calculate(
            BigDecimal loadKg,
            int repetitions,
            BigDecimal rpe
    ) {
        if (rpe.compareTo(MINIMUM_RPE_FOR_ESTIMATION) < 0) {
            return Optional.empty();
        }

        BigDecimal repsInReserve = TEN.subtract(rpe);
        BigDecimal effectiveRepetitions = BigDecimal.valueOf(repetitions).add(repsInReserve);

        if (effectiveRepetitions.compareTo(MAX_EFFECTIVE_REPETITIONS) > 0) {
            return Optional.empty();
        }

        BigDecimal estimatedOneRepMax = loadKg.multiply(
                BigDecimal.ONE.add(effectiveRepetitions.divide(EPLEY_DIVISOR, 8, RoundingMode.HALF_UP))
        ).setScale(2, RoundingMode.HALF_UP);

        return Optional.of(new EstimatedOneRepMax(
                estimatedOneRepMax,
                repsInReserve.setScale(1, RoundingMode.HALF_UP),
                effectiveRepetitions.setScale(1, RoundingMode.HALF_UP)
        ));
    }
}
