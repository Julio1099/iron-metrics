package com.ironmetrics.analytics.api;

import com.ironmetrics.analytics.domain.BodyWeightDaily;
import java.math.BigDecimal;
import java.time.LocalDate;

public record BodyWeightDailyResponse(
        LocalDate measurementDate,
        BigDecimal bodyWeightKg,
        int sourceSessions
) {

    static BodyWeightDailyResponse from(BodyWeightDaily readModel) {
        return new BodyWeightDailyResponse(
                readModel.measurementDate(),
                readModel.bodyWeightKg(),
                readModel.sourceSessions()
        );
    }
}
