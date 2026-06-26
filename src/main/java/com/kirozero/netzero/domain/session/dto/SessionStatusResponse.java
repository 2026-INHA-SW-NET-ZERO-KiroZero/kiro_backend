package com.kirozero.netzero.domain.session.dto;

import com.kirozero.netzero.domain.slot.enums.SlotStatus;
import java.util.List;

public record SessionStatusResponse(
        Long slotId,
        SlotStatus status,
        List<SessionParticipantStatusResponse> participants,
        List<SharedIngredientPoolItemResponse> sharedIngredientPool,
        boolean canRequestRecommendation
) {
}
