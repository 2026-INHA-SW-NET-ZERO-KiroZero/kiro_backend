package com.kirozero.netzero.domain.ai.model;

import com.kirozero.netzero.domain.recommendation.dto.MenuCandidateResponse;
import java.util.List;

public record CookingGuideGenerationContext(
        Long slotId,
        MenuCandidateResponse selectedMenu,
        List<AiParticipant> participants
) {
}
