package com.kirozero.netzero.domain.ai.service;

import com.kirozero.netzero.domain.ai.exception.AiGenerationException;
import com.kirozero.netzero.domain.cooking.dto.CookingGuideResponse;
import com.kirozero.netzero.domain.recommendation.dto.MenuCandidateResponse;
import java.util.List;
import java.util.Set;

final class AiResponseValidator {

    private static final Set<String> REQUIRED_LABELS = Set.of("A", "B", "C", "D");

    private AiResponseValidator() {
    }

    static void validateMenuCandidates(List<MenuCandidateResponse> candidates) {
        if (candidates == null || candidates.size() != 4) {
            throw new AiGenerationException("AI menu response must contain exactly four candidates.");
        }

        Set<String> labels = candidates.stream()
                .map(MenuCandidateResponse::candidateLabel)
                .collect(java.util.stream.Collectors.toSet());
        if (!labels.equals(REQUIRED_LABELS)) {
            throw new AiGenerationException("AI menu response must contain A, B, C, D labels.");
        }

        for (MenuCandidateResponse candidate : candidates) {
            if (isBlank(candidate.menuName()) || isBlank(candidate.menuType())) {
                throw new AiGenerationException("AI menu response contains blank menu fields.");
            }
            if (!candidate.menuType().equals("GENERAL") && !candidate.menuType().equals("LOW_CARBON")) {
                throw new AiGenerationException("AI menuType must be GENERAL or LOW_CARBON.");
            }
            if (candidate.usedLeftoverIngredients() == null
                    || candidate.commonKitItems() == null
                    || candidate.purchaseItems() == null
                    || candidate.cookingOutlineSteps() == null
                    || candidate.rolePlanSummary() == null) {
                throw new AiGenerationException("AI menu response contains null list fields.");
            }
        }
    }

    static void validateCookingGuide(CookingGuideResponse guide) {
        if (guide == null || guide.slotId() == null || isBlank(guide.menuName())) {
            throw new AiGenerationException("AI cooking guide contains blank root fields.");
        }
        if (guide.steps() == null || guide.steps().isEmpty()) {
            throw new AiGenerationException("AI cooking guide must contain steps.");
        }
        for (var step : guide.steps()) {
            if (isBlank(step.phase()) || isBlank(step.title()) || isBlank(step.instruction())) {
                throw new AiGenerationException("AI cooking guide contains blank step fields.");
            }
            if (step.participantTasks() == null || step.participantTasks().isEmpty()) {
                throw new AiGenerationException("AI cooking guide step must contain participant tasks.");
            }
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
