package com.kirozero.netzero.domain.session.dto;

import com.kirozero.netzero.domain.session.entity.SessionIngredient;
import java.math.BigDecimal;

public record SessionIngredientStatusResponse(
        Long ingredientId,
        String nameKo,
        BigDecimal count,
        BigDecimal knownGrams,
        BigDecimal estimatedGrams
) {

    public static SessionIngredientStatusResponse from(SessionIngredient ingredient) {
        return new SessionIngredientStatusResponse(
                ingredient.getIngredient().getId(),
                ingredient.getIngredient().getNameKo(),
                ingredient.getCount(),
                ingredient.getKnownGrams(),
                ingredient.getEstimatedGrams()
        );
    }
}
