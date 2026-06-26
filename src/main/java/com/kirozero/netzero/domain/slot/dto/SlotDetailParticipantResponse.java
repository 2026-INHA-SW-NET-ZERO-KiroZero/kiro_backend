package com.kirozero.netzero.domain.slot.dto;

import com.kirozero.netzero.domain.session.entity.SessionParticipant;
import com.kirozero.netzero.domain.user.enums.CookingSkill;

public record SlotDetailParticipantResponse(
        Long participantId,
        String nickname,
        CookingSkill cookingSkill,
        boolean canPurchase,
        int ingredientCount
) {

    public static SlotDetailParticipantResponse from(SessionParticipant participant, int ingredientCount) {
        return new SlotDetailParticipantResponse(
                participant.getId(),
                participant.getUser().getNickname(),
                participant.getUser().getCookingSkill(),
                participant.isCanPurchase(),
                ingredientCount
        );
    }
}
