package com.kirozero.netzero.domain.cooking.dto;

import java.util.List;

public record CookingGuideStepResponse(
        int stepOrder,
        String phase,
        String title,
        int estimatedMinutes,
        String instruction,
        List<CookingUsedIngredientResponse> usedIngredients,
        List<String> tools,
        List<String> kitItems,
        List<ParticipantTaskResponse> participantTasks,
        String safetyNote,
        String completionCriteria
) {
}
