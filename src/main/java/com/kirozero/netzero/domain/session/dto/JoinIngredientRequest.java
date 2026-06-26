package com.kirozero.netzero.domain.session.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record JoinIngredientRequest(
        @NotNull Long ingredientId,
        @NotNull @DecimalMin(value = "0.01") BigDecimal count,
        @DecimalMin(value = "0.01") BigDecimal knownGrams
) {
}
