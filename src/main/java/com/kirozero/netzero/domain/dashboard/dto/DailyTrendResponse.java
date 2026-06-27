package com.kirozero.netzero.domain.dashboard.dto;

import java.util.List;

public record DailyTrendResponse(
        List<String> labels,
        OperationSeries operation,
        EnvironmentSeries environment
) {

    public record OperationSeries(
            List<Long> sessions,
            List<Long> participants
    ) {
    }

    public record EnvironmentSeries(
            List<Double> ingredientUsedKg,
            List<Double> carbonReductionKgco2e
    ) {
    }
}
