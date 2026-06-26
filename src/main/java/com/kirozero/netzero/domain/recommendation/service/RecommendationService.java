package com.kirozero.netzero.domain.recommendation.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kirozero.netzero.domain.ai.model.AiIngredient;
import com.kirozero.netzero.domain.ai.model.AiParticipant;
import com.kirozero.netzero.domain.ai.model.MenuCandidateGenerationContext;
import com.kirozero.netzero.domain.ai.port.AiGenerationPort;
import com.kirozero.netzero.domain.auth.service.AuthService;
import com.kirozero.netzero.domain.recommendation.dto.LatestRecommendationResponse;
import com.kirozero.netzero.domain.recommendation.dto.MenuCandidateResponse;
import com.kirozero.netzero.domain.recommendation.dto.RecommendationRequest;
import com.kirozero.netzero.domain.recommendation.dto.RecommendationResponse;
import com.kirozero.netzero.domain.session.entity.SessionIngredient;
import com.kirozero.netzero.domain.session.entity.SessionParticipant;
import com.kirozero.netzero.domain.session.repository.SessionIngredientRepository;
import com.kirozero.netzero.domain.session.repository.SessionParticipantRepository;
import com.kirozero.netzero.domain.slot.entity.Slot;
import com.kirozero.netzero.domain.slot.enums.SlotStatus;
import com.kirozero.netzero.domain.slot.repository.SlotRepository;
import com.kirozero.netzero.domain.user.entity.User;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class RecommendationService {

    private static final List<String> COMMON_KIT = List.of("식용유", "간장", "소금", "후추", "참기름");

    private final AuthService authService;
    private final SlotRepository slotRepository;
    private final SessionParticipantRepository sessionParticipantRepository;
    private final SessionIngredientRepository sessionIngredientRepository;
    private final AiGenerationPort aiGenerationPort;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Transactional
    public RecommendationResponse recommend(Long slotId, String authorizationHeader, RecommendationRequest request) {
        User user = authService.requireUser(authorizationHeader);
        Slot slot = slotRepository.findById(slotId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Slot not found."));
        List<SessionParticipant> participants = sessionParticipantRepository.findBySlotIdOrderByJoinedAtAsc(slotId);
        List<SessionIngredient> ingredients = sessionIngredientRepository.findBySlotIdOrderByIdAsc(slotId);

        validateRecommendationRequest(slot, user, participants, ingredients);

        MenuCandidateGenerationContext context = new MenuCandidateGenerationContext(
                slot.getId(),
                buildPoolItems(ingredients),
                buildParticipants(participants),
                COMMON_KIT
        );
        List<MenuCandidateResponse> candidates = aiGenerationPort.generateMenuCandidates(context);
        slot.proposeMenuCandidates(writeCandidatesJson(candidates));

        return new RecommendationResponse(
                slot.getId(),
                slot.getRecommendationCount(),
                slot.getStatus(),
                candidates
        );
    }

    @Transactional(readOnly = true)
    public LatestRecommendationResponse getLatestRecommendation(Long slotId, String authorizationHeader) {
        User user = authService.requireUser(authorizationHeader);
        Slot slot = slotRepository.findById(slotId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Slot not found."));

        if (!sessionParticipantRepository.existsBySlotIdAndUserId(slotId, user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only session participants can view recommendations.");
        }

        return new LatestRecommendationResponse(
                slot.getId(),
                slot.getRecommendationCount(),
                slot.getStatus(),
                readCandidates(slot.getCandidatesJson()),
                readSelectedMenu(slot.getSelectedMenuJson())
        );
    }

    private void validateRecommendationRequest(
            Slot slot,
            User user,
            List<SessionParticipant> participants,
            List<SessionIngredient> ingredients
    ) {
        if (slot.getStatus() != SlotStatus.OPEN) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Recommendations can only be requested for OPEN slots.");
        }

        boolean participant = participants.stream()
                .anyMatch(item -> item.getUser().getId().equals(user.getId()));
        if (!participant) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only session participants can request recommendations.");
        }

        if (participants.size() != slot.getCapacity()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Slot must be full before recommendation.");
        }

        Map<Long, Long> ingredientCountByParticipantId = new LinkedHashMap<>();
        for (SessionIngredient ingredient : ingredients) {
            ingredientCountByParticipantId.merge(ingredient.getParticipant().getId(), 1L, Long::sum);
        }

        boolean allHaveIngredients = participants.stream()
                .allMatch(item -> ingredientCountByParticipantId.getOrDefault(item.getId(), 0L) > 0);
        if (!allHaveIngredients) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Every participant must have at least one ingredient.");
        }
    }

    private List<AiIngredient> buildPoolItems(List<SessionIngredient> ingredients) {
        Map<Long, IngredientPoolItemAccumulator> poolByIngredientId = new LinkedHashMap<>();
        for (SessionIngredient ingredient : ingredients) {
            poolByIngredientId
                    .computeIfAbsent(
                            ingredient.getIngredient().getId(),
                            ignored -> new IngredientPoolItemAccumulator(
                                    ingredient.getIngredient().getId(),
                                    ingredient.getIngredient().getNameKo()
                            )
                    )
                    .add(ingredient.getEstimatedGrams());
        }

        return poolByIngredientId.values().stream()
                .sorted(Comparator.comparing(IngredientPoolItemAccumulator::estimatedTotalGrams).reversed())
                .map(item -> new AiIngredient(item.ingredientId(), item.nameKo(), item.estimatedTotalGrams()))
                .toList();
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

    private String writeCandidatesJson(List<MenuCandidateResponse> candidates) {
        try {
            return objectMapper.writeValueAsString(candidates);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Candidates cannot be serialized.", e);
        }
    }

    private List<MenuCandidateResponse> readCandidates(String candidatesJson) {
        if (!StringUtils.hasText(candidatesJson)) {
            return List.of();
        }

        try {
            return objectMapper.readValue(unwrapStoredJson(candidatesJson), new TypeReference<>() {
            });
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Menu candidates cannot be parsed.", e);
        }
    }

    private MenuCandidateResponse readSelectedMenu(String selectedMenuJson) {
        if (!StringUtils.hasText(selectedMenuJson)) {
            return null;
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

    private static class IngredientPoolItemAccumulator {

        private final Long ingredientId;
        private final String nameKo;
        private java.math.BigDecimal estimatedTotalGrams = java.math.BigDecimal.ZERO;

        private IngredientPoolItemAccumulator(Long ingredientId, String nameKo) {
            this.ingredientId = ingredientId;
            this.nameKo = nameKo;
        }

        private Long ingredientId() {
            return ingredientId;
        }

        private String nameKo() {
            return nameKo;
        }

        private java.math.BigDecimal estimatedTotalGrams() {
            return estimatedTotalGrams;
        }

        private void add(java.math.BigDecimal grams) {
            estimatedTotalGrams = estimatedTotalGrams.add(grams);
        }
    }
}
