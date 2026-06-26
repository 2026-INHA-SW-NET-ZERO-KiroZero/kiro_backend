package com.kirozero.netzero.domain.result.dto;

import java.math.BigDecimal;

public record MonthlyResultSummaryResponse(
        String yearMonth,
        int completedSessionCount,
        BigDecimal totalUsedGrams,
        BigDecimal totalEstimatedCarbonSavedKgco2e,
        int totalRefundAmount
) {
}
