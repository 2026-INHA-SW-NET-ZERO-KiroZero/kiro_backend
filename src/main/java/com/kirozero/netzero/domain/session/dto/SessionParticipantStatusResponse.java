package com.kirozero.netzero.domain.session.dto;

import com.kirozero.netzero.domain.session.entity.SessionParticipant;
import com.kirozero.netzero.domain.user.entity.UserAllergy;
import com.kirozero.netzero.domain.user.enums.CookingSkill;
import java.util.List;

public record SessionParticipantStatusResponse(
        Long participantId,
        String nickname,
        CookingSkill cookingSkill,
        List<String> allergyTags,
        boolean canPurchase,
        List<SessionIngredientStatusResponse> ingredients
) {

    public static SessionParticipantStatusResponse from(
            SessionParticipant participant,
            List<SessionIngredientStatusResponse> ingredients
    ) {
        return new SessionParticipantStatusResponse(
                participant.getId(),
                participant.getUser().getNickname(),
                participant.getUser().getCookingSkill(),
                participant.getUser().getAllergies().stream()
                        .map(UserAllergy::getAllergenTag)
                        .toList(),
                participant.isCanPurchase(),
                ingredients
        );
    }
}
