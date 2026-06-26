package com.kirozero.netzero.domain.vote.dto;

import com.kirozero.netzero.domain.slot.enums.SlotStatus;
import java.util.Map;

public record MenuVoteResponse(
        Long slotId,
        int recommendationCount,
        Map<String, Long> voteSummary,
        boolean confirmed,
        String confirmedCandidateLabel,
        SelectedMenuSummaryResponse selectedMenu,
        SlotStatus nextStatus
) {
}
