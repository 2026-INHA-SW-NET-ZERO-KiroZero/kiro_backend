package com.kirozero.netzero.domain.cooking.dto;

import java.math.BigDecimal;

public record CookingUsedIngredientResponse(
        Long ingredientId,
        String nameKo,
        BigDecimal plannedUseGrams
) {
}
