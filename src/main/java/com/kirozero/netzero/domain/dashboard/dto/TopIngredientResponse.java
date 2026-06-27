package com.kirozero.netzero.domain.dashboard.dto;

public record TopIngredientResponse(
        String ingredientName,
        double inputGrams,
        double leftoverGrams,
        double leftoverRate
) {
}
