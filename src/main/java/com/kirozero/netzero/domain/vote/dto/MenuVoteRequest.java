package com.kirozero.netzero.domain.vote.dto;

import com.kirozero.netzero.domain.vote.enums.VoteType;
import jakarta.validation.constraints.NotNull;

public record MenuVoteRequest(
        @NotNull VoteType voteType,
        String candidateLabel,
        String reasonText
) {
}
