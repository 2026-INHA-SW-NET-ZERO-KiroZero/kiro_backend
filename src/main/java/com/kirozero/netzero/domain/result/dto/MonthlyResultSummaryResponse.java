package com.kirozero.netzero.domain.result.dto;

import java.math.BigDecimal;

public record MonthlyResultSummaryResponse(
        String yearMonth,
        String monthLabel,
        int completedSessionCount,
        BigDecimal totalUsedGrams,
        BigDecimal totalEstimatedCarbonSavedKgco2e,
        int totalRefundAmount,
        int togetherPeopleCount,
        int providedIngredientCount,
        int usedIngredientCount,
        int averageIngredientUseRate
) {
}
