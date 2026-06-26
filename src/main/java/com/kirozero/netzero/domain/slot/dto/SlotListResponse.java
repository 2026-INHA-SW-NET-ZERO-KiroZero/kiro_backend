package com.kirozero.netzero.domain.slot.dto;

import java.util.List;

public record SlotListResponse(
        List<SlotListItemResponse> slots
) {
}
