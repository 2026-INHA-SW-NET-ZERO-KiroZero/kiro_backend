package com.kirozero.netzero.domain.ai.model;

import java.util.List;

public record AiParticipant(
        Long participantId,
        String nickname,
        String cookingSkill,
        boolean canPurchase,
        List<String> allergyTags
) {
}
