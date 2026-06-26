package com.kirozero.netzero.domain.recommendation.dto;

import java.util.List;

public record MenuCandidateResponse(
        String candidateLabel,
        String menuName,
        String menuType,
        List<CandidateUsedIngredientResponse> usedLeftoverIngredients,
        List<String> commonKitItems,
        List<PurchaseItemResponse> purchaseItems,
        int cookingTimeMinutes,
        String difficulty,
        String recommendationReason,
        List<String> cookingOutlineSteps,
        List<String> rolePlanSummary
) {
}
