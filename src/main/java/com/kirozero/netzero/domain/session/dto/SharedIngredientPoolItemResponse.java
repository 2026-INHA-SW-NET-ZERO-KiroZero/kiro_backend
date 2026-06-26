package com.kirozero.netzero.domain.session.dto;

import java.math.BigDecimal;

public record SharedIngredientPoolItemResponse(
        Long ingredientId,
        String nameKo,
        BigDecimal estimatedTotalGrams
) {
}
