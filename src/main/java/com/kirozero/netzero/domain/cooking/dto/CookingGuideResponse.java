package com.kirozero.netzero.domain.cooking.dto;

import java.util.List;

public record CookingGuideResponse(
        Long slotId,
        String menuName,
        List<CookingGuideStepResponse> steps
) {
}
