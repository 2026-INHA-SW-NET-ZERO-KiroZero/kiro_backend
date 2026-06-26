package com.kirozero.netzero.domain.session.dto;

import com.kirozero.netzero.domain.slot.enums.SlotStatus;

public record LeaveSlotResponse(
        Long slotId,
        SlotStatus status,
        boolean left
) {
}
