package com.ironmetrics.analytics.api;

import com.ironmetrics.analytics.domain.TrainingVolumeDaily;
import java.math.BigDecimal;
import java.time.LocalDate;

public record TrainingVolumeDailyResponse(
        LocalDate trainingDate,
        int totalSets,
        int totalRepetitions,
        BigDecimal totalVolumeKg,
        BigDecimal averageRpe,
        BigDecimal maxEstimatedOneRepMaxKg
) {

    static TrainingVolumeDailyResponse from(TrainingVolumeDaily readModel) {
        return new TrainingVolumeDailyResponse(
                readModel.trainingDate(),
                readModel.totalSets(),
                readModel.totalRepetitions(),
                readModel.totalVolumeKg(),
                readModel.averageRpe(),
                readModel.maxEstimatedOneRepMaxKg()
        );
    }
}
