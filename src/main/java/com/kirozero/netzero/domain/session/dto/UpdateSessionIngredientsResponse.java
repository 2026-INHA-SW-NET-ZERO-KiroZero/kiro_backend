package com.kirozero.netzero.domain.session.dto;

import java.util.List;

public record UpdateSessionIngredientsResponse(
        Long slotId,
        List<SessionIngredientResponse> items
) {
}
