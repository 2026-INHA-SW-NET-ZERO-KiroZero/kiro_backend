package com.kirozero.netzero.domain.ingredient.dto;

import com.kirozero.netzero.domain.ingredient.entity.IngredientMaster;
import java.math.BigDecimal;

public record IngredientItemResponse(
        Long ingredientId,
        String nameKo,
        BigDecimal gramsPerCount
) {

    public static IngredientItemResponse from(IngredientMaster ingredient) {
        return new IngredientItemResponse(
                ingredient.getId(),
                ingredient.getNameKo(),
                ingredient.getGramsPerCount()
        );
    }
}
