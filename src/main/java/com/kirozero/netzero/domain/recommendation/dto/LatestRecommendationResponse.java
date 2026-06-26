package com.kirozero.netzero.domain.recommendation.dto;

import com.kirozero.netzero.domain.slot.enums.SlotStatus;
import java.util.List;

public record LatestRecommendationResponse(
        Long slotId,
        int recommendationCount,
        SlotStatus status,
        List<MenuCandidateResponse> candidates,
        MenuCandidateResponse selectedMenu
) {
}
