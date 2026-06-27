package com.kirozero.netzero.domain.dashboard.dto;

public record ImpactSummaryResponse(
        double totalIngredientUsedKg,
        double estimatedCarbonReductionKgco2e,
        long totalParticipants,
        long totalCompletedSessions,
        double treeEquivalent,
        double averageIngredientUseRate,
        double lowCarbonMenuSelectionRate,
        long totalRefundAmount,
        double averageRefundAmount
) {
}
