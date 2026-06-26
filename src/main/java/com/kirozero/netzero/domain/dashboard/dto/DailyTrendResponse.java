package com.kirozero.netzero.domain.dashboard.dto;

import java.util.List;

public record DailyTrendResponse(
        List<String> labels,
        Series series
) {

    public record Series(
            List<Long> sessions,
            List<Long> participants,
            List<Double> foodGrams,
            List<Double> carbonKg
    ) {
    }
}
