package com.kirozero.netzero.domain.result.dto;

import java.math.BigDecimal;
import java.util.List;

public record MyResultTotalResponse(
        int completedSessionCount,
        BigDecimal totalUsedGrams,
        BigDecimal totalEstimatedCarbonSavedKgco2e,
        int totalRefundAmount,
        int togetherPeopleCount,
        int providedIngredientCount,
        int usedIngredientCount,
        int averageIngredientUseRate,
        BigDecimal currentMonthEstimatedCarbonSavedKgco2e,
        BigDecimal previousMonthEstimatedCarbonSavedKgco2e,
        BigDecimal monthOverMonthCarbonDeltaKgco2e,
        String insightMessage,
        List<MonthlyResultSummaryResponse> monthlyResults
) {
}
