package com.kirozero.netzero.domain.result.dto;

import java.math.BigDecimal;

public record SessionResultResponse(
        Long slotId,
        String menuName,
        String menuType,
        BigDecimal totalUsedGrams,
        int avgIngredientUseRate,
        int finishedFoodRate,
        BigDecimal estimatedFoodWasteReducedGrams,
        BigDecimal estimatedCarbonSavedKgco2e,
        boolean lowCarbonSelected,
        int refundScore,
        int refundAmountPerUser,
        int totalRefundAmount,
        String summaryText,
        PhotoUrlsResponse photoUrls
) {
}
