package com.kirozero.netzero.domain.vote.dto;

public record SelectedMenuSummaryResponse(
        String candidateLabel,
        String menuName,
        String menuType
) {
}
