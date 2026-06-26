package com.kirozero.netzero.domain.ai.service;

import com.kirozero.netzero.domain.ai.exception.AiGenerationException;
import com.kirozero.netzero.domain.ai.model.RawMenuCandidate;
import com.kirozero.netzero.domain.cooking.dto.CookingGuideResponse;
import java.util.List;
import java.util.Set;

final class AiResponseValidator {

    private static final int RAW_CANDIDATE_COUNT = 6;
    private static final int EXPECTED_GENERAL = 3;
    private static final int EXPECTED_LOW_CARBON = 3;
    private static final Set<String> ALLOWED_TYPES = Set.of("GENERAL", "LOW_CARBON");

    private AiResponseValidator() {
    }

    static void validateRawCandidates(List<RawMenuCandidate> candidates) {
        if (candidates == null || candidates.size() != RAW_CANDIDATE_COUNT) {
            throw new AiGenerationException(
                    "AI raw menu response must contain exactly " + RAW_CANDIDATE_COUNT + " candidates."
            );
        }

        long general = candidates.stream().filter(c -> "GENERAL".equals(c.menuType())).count();
        long lowCarbon = candidates.stream().filter(c -> "LOW_CARBON".equals(c.menuType())).count();
        if (general != EXPECTED_GENERAL || lowCarbon != EXPECTED_LOW_CARBON) {
            throw new AiGenerationException(
                    "AI raw menu response must contain 3 GENERAL and 3 LOW_CARBON candidates."
            );
        }

        for (RawMenuCandidate candidate : candidates) {
            if (isBlank(candidate.menuName()) || isBlank(candidate.menuType())) {
                throw new AiGenerationException("AI menu response contains blank menu fields.");
            }
            if (!ALLOWED_TYPES.contains(candidate.menuType())) {
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
