package com.kirozero.netzero.domain.result.dto;

import java.math.BigDecimal;

public record MyResultTotalResponse(
        int completedSessionCount,
        BigDecimal totalUsedGrams,
        BigDecimal totalEstimatedCarbonSavedKgco2e,
        int totalRefundAmount
) {
}
