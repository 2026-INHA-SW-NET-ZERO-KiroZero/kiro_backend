package com.kirozero.netzero.domain.session.dto;

import com.kirozero.netzero.domain.slot.enums.SlotStatus;
import java.util.List;

public record JoinSlotResponse(
        Long slotId,
        Long participantId,
        SlotStatus status,
        boolean canPurchase,
        List<SessionIngredientResponse> ingredients
) {
}
