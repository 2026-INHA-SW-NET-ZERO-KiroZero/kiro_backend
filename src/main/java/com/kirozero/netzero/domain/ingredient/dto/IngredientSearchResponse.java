package com.kirozero.netzero.domain.ingredient.dto;

import java.util.List;

public record IngredientSearchResponse(
        List<IngredientItemResponse> ingredients
) {
}
