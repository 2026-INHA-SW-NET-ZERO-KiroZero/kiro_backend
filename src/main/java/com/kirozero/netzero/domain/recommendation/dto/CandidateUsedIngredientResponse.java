package com.kirozero.netzero.domain.recommendation.dto;

import java.math.BigDecimal;

public record CandidateUsedIngredientResponse(
        Long ingredientId,
        String nameKo,
        BigDecimal availableGrams,
        BigDecimal plannedUseGrams,
        BigDecimal estimatedUseRatio
) {
}
