package com.kirozero.netzero.domain.slot.dto;

import com.kirozero.netzero.domain.session.entity.SessionParticipant;
import com.kirozero.netzero.domain.user.entity.UserAllergy;
import com.kirozero.netzero.domain.user.enums.CookingSkill;
import java.util.List;

public record SlotDetailParticipantResponse(
        Long participantId,
        String nickname,
        CookingSkill cookingSkill,
        List<String> allergyTags,
        boolean canPurchase,
        int ingredientCount
) {

    public static SlotDetailParticipantResponse from(SessionParticipant participant, int ingredientCount) {
        return new SlotDetailParticipantResponse(
                participant.getId(),
                participant.getUser().getNickname(),
                participant.getUser().getCookingSkill(),
                participant.getUser().getAllergies().stream()
                        .map(UserAllergy::getAllergenTag)
                        .toList(),
                participant.isCanPurchase(),
                ingredientCount
        );
    }
}
