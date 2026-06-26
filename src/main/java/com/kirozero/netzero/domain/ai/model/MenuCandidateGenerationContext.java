package com.kirozero.netzero.domain.ai.model;

import java.util.List;

public record MenuCandidateGenerationContext(
        Long slotId,
        List<AiIngredient> sharedIngredients,
        List<AiParticipant> participants,
        List<String> commonKitItems
) {
}
