package com.kirozero.netzero.domain.cooking.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kirozero.netzero.domain.ai.model.AiParticipant;
import com.kirozero.netzero.domain.ai.model.CookingGuideGenerationContext;
import com.kirozero.netzero.domain.ai.port.AiGenerationPort;
import com.kirozero.netzero.domain.auth.service.AuthService;
import com.kirozero.netzero.domain.cooking.dto.CookingGuideResponse;
import com.kirozero.netzero.domain.cooking.dto.CookingGuideStepResponse;
import com.kirozero.netzero.domain.recommendation.dto.MenuCandidateResponse;
import com.kirozero.netzero.domain.session.entity.SessionParticipant;
import com.kirozero.netzero.domain.session.repository.SessionParticipantRepository;
import com.kirozero.netzero.domain.slot.entity.Slot;
import com.kirozero.netzero.domain.slot.repository.SlotRepository;
import com.kirozero.netzero.domain.user.entity.User;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class CookingGuideService {

    private final AuthService authService;
    private final SlotRepository slotRepository;
    private final SessionParticipantRepository sessionParticipantRepository;
    private final AiGenerationPort aiGenerationPort;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Transactional
    public CookingGuideResponse getCookingGuide(Long slotId, String authorizationHeader, String view) {
        User user = authService.requireUser(authorizationHeader);
        Slot slot = slotRepository.findById(slotId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Slot not found."));
        SessionParticipant currentParticipant = sessionParticipantRepository.findBySlotIdAndUserId(slotId, user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Only participants can view cooking guide."));
        MenuCandidateResponse selectedMenu = readSelectedMenu(slot.getSelectedMenuJson());

        CookingGuideResponse guide = readOrCreateGuide(slot, selectedMenu);
        if (view == null || view.isBlank() || "all".equalsIgnoreCase(view)) {
            return guide;
        }
        if ("mine".equalsIgnoreCase(view)) {
            return filterMine(guide, currentParticipant.getId());
        }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "view must be all or mine.");
    }

    private CookingGuideResponse readOrCreateGuide(Slot slot, MenuCandidateResponse selectedMenu) {
        if (StringUtils.hasText(slot.getCookingPlanJson())) {
            try {
                return objectMapper.readValue(unwrapStoredJson(slot.getCookingPlanJson()), CookingGuideResponse.class);
            } catch (JsonProcessingException e) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Cooking guide cannot be parsed.", e);
            }
        }

        List<SessionParticipant> participants = sessionParticipantRepository.findBySlotIdOrderByJoinedAtAsc(slot.getId());
        CookingGuideResponse guide = aiGenerationPort.generateCookingGuide(new CookingGuideGenerationContext(
                slot.getId(),
                selectedMenu,
                buildParticipants(participants)
        ));
        try {
            slot.saveCookingPlan(objectMapper.writeValueAsString(guide));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Cooking guide cannot be serialized.", e);
        }
        return guide;
    }

    private List<AiParticipant> buildParticipants(List<SessionParticipant> participants) {
        return participants.stream()
                .map(participant -> new AiParticipant(
                        participant.getId(),
                        participant.getUser().getNickname(),
                        participant.getUser().getCookingSkill().name(),
                        participant.isCanPurchase(),
                        participant.getUser().getAllergies().stream()
                                .map(allergy -> allergy.getAllergenTag())
                                .toList()
                ))
                .toList();
    }

    private CookingGuideResponse filterMine(CookingGuideResponse guide, Long participantId) {
        return new CookingGuideResponse(
                guide.slotId(),
                guide.menuName(),
                guide.steps().stream()
                        .map(step -> new CookingGuideStepResponse(
                                step.stepOrder(),
                                step.phase(),
                                step.title(),
                                step.estimatedMinutes(),
                                step.instruction(),
                                step.usedIngredients(),
                                step.tools(),
                                step.kitItems(),
                                step.participantTasks().stream()
                                        .filter(task -> task.participantId().equals(participantId))
                                        .toList(),
                                step.safetyNote(),
                                step.completionCriteria()
                        ))
                        .toList()
        );
    }

    private MenuCandidateResponse readSelectedMenu(String selectedMenuJson) {
        if (!StringUtils.hasText(selectedMenuJson)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Selected menu is required.");
        }

        try {
            return objectMapper.readValue(unwrapStoredJson(selectedMenuJson), MenuCandidateResponse.class);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Selected menu cannot be parsed.", e);
        }
    }

    private String unwrapStoredJson(String value) throws JsonProcessingException {
        String unwrapped = value.trim();
        for (int i = 0; i < 3; i++) {
            if (!unwrapped.startsWith("\"") || !unwrapped.endsWith("\"")) {
                return unwrapped;
            }
            unwrapped = objectMapper.readValue(unwrapped, String.class).trim();
        }
        return unwrapped;
    }
}
