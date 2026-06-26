package com.kirozero.netzero.domain.session.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record JoinSlotRequest(
        boolean canPurchase,
        @NotEmpty List<@Valid JoinIngredientRequest> ingredients
) {
}
