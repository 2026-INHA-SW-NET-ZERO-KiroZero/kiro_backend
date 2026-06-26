package com.kirozero.netzero.domain.dashboard.dto;

public record ImpactSummaryResponse(
        double totalFoodProcessedKg,
        double totalCarbonSavedKgco2e,
        long totalParticipants,
        long totalCompletedSessions,
        double treeEquivalent
) {
}
