package com.kirozero.netzero.domain.session.dto;

import com.kirozero.netzero.domain.slot.enums.SlotStatus;
import com.kirozero.netzero.domain.vote.dto.SelectedMenuSummaryResponse;
import java.util.List;

public record MySessionDetailResponse(
        Long slotId,
        Long myParticipantId,
        boolean joined,
        boolean canPurchase,
        SlotStatus status,
        List<SessionIngredientResponse> myIngredients,
        SessionStatusResponse session,
        boolean hasRecommendation,
        boolean hasSelectedMenu,
        boolean completed,
        SelectedMenuSummaryResponse selectedMenu
) {
}
