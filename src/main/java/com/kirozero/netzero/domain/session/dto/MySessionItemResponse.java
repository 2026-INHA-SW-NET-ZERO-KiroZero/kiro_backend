package com.kirozero.netzero.domain.session.dto;

import com.kirozero.netzero.domain.slot.enums.SlotStatus;
import com.kirozero.netzero.domain.vote.dto.SelectedMenuSummaryResponse;

public record MySessionItemResponse(
        Long slotId,
        Long participantId,
        String date,
        String placeName,
        String stationCode,
        String startTime,
        String endTime,
        String timeLabel,
        int capacity,
        long participantCount,
        SlotStatus status,
        boolean canPurchase,
        long myIngredientCount,
        boolean hasRecommendation,
        boolean hasSelectedMenu,
        boolean completed,
        SelectedMenuSummaryResponse selectedMenu
) {
}
