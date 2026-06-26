package com.kirozero.netzero.domain.result.dto;

import jakarta.validation.constraints.NotNull;

public record ConsumptionRecordItemRequest(
        @NotNull Long sessionIngredientId,
        @NotNull Integer useRate
) {
}
