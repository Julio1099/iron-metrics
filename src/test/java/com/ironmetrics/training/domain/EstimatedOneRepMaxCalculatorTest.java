package com.ironmetrics.training.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class EstimatedOneRepMaxCalculatorTest {

    private final EstimatedOneRepMaxCalculator calculator = new EstimatedOneRepMaxCalculator();

    @Test
    void shouldCalculateEstimatedOneRepMaxUsingEffectiveRepetitions() {
        EstimatedOneRepMax result = calculator.calculate(
                new BigDecimal("100.00"),
                5,
                new BigDecimal("9.0")
        ).orElseThrow();

        assertThat(result.repsInReserve()).isEqualByComparingTo("1.0");
        assertThat(result.effectiveRepetitions()).isEqualByComparingTo("6.0");
        assertThat(result.value()).isEqualByComparingTo("120.00");
    }

    @Test
    void shouldIgnoreEstimatedOneRepMaxWhenRpeIsBelowSeven() {
        assertThat(calculator.calculate(
                new BigDecimal("100.00"),
                8,
                new BigDecimal("6.5")
        )).isEmpty();
    }

    @Test
    void shouldIgnoreEstimatedOneRepMaxWhenEffectiveRepetitionsAreAboveTwelve() {
        assertThat(calculator.calculate(
                new BigDecimal("80.00"),
                10,
                new BigDecimal("7.0")
        )).isEmpty();
    }

    @Test
    void shouldAllowEstimatedOneRepMaxWhenEffectiveRepetitionsAreExactlyTwelve() {
        EstimatedOneRepMax result = calculator.calculate(
                new BigDecimal("80.00"),
                9,
                new BigDecimal("7.0")
        ).orElseThrow();

        assertThat(result.repsInReserve()).isEqualByComparingTo("3.0");
        assertThat(result.effectiveRepetitions()).isEqualByComparingTo("12.0");
        assertThat(result.value()).isEqualByComparingTo("112.00");
    }
}
