package com.kirozero.netzero.domain.ai.model;

import java.math.BigDecimal;

public record AiIngredient(
        Long ingredientId,
        String nameKo,
        BigDecimal availableGrams
) {
}
