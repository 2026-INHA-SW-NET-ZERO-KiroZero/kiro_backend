package com.kirozero.netzero.domain.session.dto;

import com.kirozero.netzero.domain.session.entity.SessionIngredient;
import java.math.BigDecimal;

public record SessionIngredientResponse(
        Long sessionIngredientId,
        Long ingredientId,
        String nameKo,
        BigDecimal count,
        BigDecimal knownGrams,
        BigDecimal estimatedGrams
) {

    public static SessionIngredientResponse from(SessionIngredient sessionIngredient) {
        return new SessionIngredientResponse(
                sessionIngredient.getId(),
                sessionIngredient.getIngredient().getId(),
                sessionIngredient.getIngredient().getNameKo(),
                sessionIngredient.getCount(),
                sessionIngredient.getKnownGrams(),
                sessionIngredient.getEstimatedGrams()
        );
    }
}
