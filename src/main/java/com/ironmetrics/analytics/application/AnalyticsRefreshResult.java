package com.ironmetrics.analytics.application;

public record AnalyticsRefreshResult(
        int trainingVolumeRows,
        int exerciseProgressionRows,
        int bodyWeightRows
) {
}
