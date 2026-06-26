package com.kirozero.netzero.domain.session.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kirozero.netzero.domain.auth.service.AuthService;
import com.kirozero.netzero.domain.recommendation.dto.MenuCandidateResponse;
import com.kirozero.netzero.domain.session.dto.SessionIngredientStatusResponse;
import com.kirozero.netzero.domain.session.dto.SessionParticipantStatusResponse;
import com.kirozero.netzero.domain.session.dto.SessionChecklistResponse;
import com.kirozero.netzero.domain.session.dto.SessionIngredientResponse;
import com.kirozero.netzero.domain.session.dto.SessionStatusResponse;
import com.kirozero.netzero.domain.session.dto.SharedIngredientPoolItemResponse;
import com.kirozero.netzero.domain.session.entity.SessionIngredient;
import com.kirozero.netzero.domain.session.entity.SessionParticipant;
import com.kirozero.netzero.domain.session.repository.SessionIngredientRepository;
import com.kirozero.netzero.domain.session.repository.SessionParticipantRepository;
import com.kirozero.netzero.domain.slot.entity.Slot;
import com.kirozero.netzero.domain.slot.enums.SlotStatus;
import com.kirozero.netzero.domain.slot.repository.SlotRepository;
import com.kirozero.netzero.domain.user.entity.User;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class SessionQueryService {

    private static final int RESERVATION_CREDIT = 2000;

    private final AuthService authService;
    private final SlotRepository slotRepository;
    private final SessionParticipantRepository sessionParticipantRepository;
    private final SessionIngredientRepository sessionIngredientRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Transactional(readOnly = true)
    public SessionStatusResponse getSessionStatus(Long slotId) {
        Slot slot = slotRepository.findById(slotId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Slot not found."));
        List<SessionParticipant> participants = sessionParticipantRepository.findBySlotIdOrderByJoinedAtAsc(slotId);
        Map<Long, List<SessionIngredient>> ingredientsByParticipantId = loadIngredientsByParticipantId(slotId);

        List<SessionParticipantStatusResponse> participantResponses = participants.stream()
                .map(participant -> SessionParticipantStatusResponse.from(
                        participant,
                        ingredientsByParticipantId.getOrDefault(participant.getId(), List.of()).stream()
                                .map(SessionIngredientStatusResponse::from)
                                .toList()
                ))
                .toList();

        return new SessionStatusResponse(
                slot.getId(),
                slot.getStatus(),
                participantResponses,
                buildSharedIngredientPool(ingredientsByParticipantId),
                canRequestRecommendation(slot, participants, ingredientsByParticipantId)
        );
    }

    @Transactional(readOnly = true)
    public SessionChecklistResponse getChecklist(Long slotId, String authorizationHeader) {
        User user = authService.requireUser(authorizationHeader);
        Slot slot = slotRepository.findById(slotId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Slot not found."));
        SessionParticipant participant = sessionParticipantRepository.findBySlotIdAndUserId(slotId, user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Only participants can view checklist."));

        MenuCandidateResponse selectedMenu = readSelectedMenu(slot.getSelectedMenuJson());

        return new SessionChecklistResponse(
                slot.getId(),
                selectedMenu.menuName(),
                selectedMenu.menuType(),
                sessionIngredientRepository.findByParticipantIdOrderByIdAsc(participant.getId()).stream()
                        .map(SessionIngredientResponse::from)
                        .toList(),
                selectedMenu.commonKitItems() == null ? List.of() : selectedMenu.commonKitItems(),
                selectedMenu.purchaseItems() == null ? List.of() : selectedMenu.purchaseItems(),
                RESERVATION_CREDIT,
                refundHint(selectedMenu.menuType())
        );
    }

    private Map<Long, List<SessionIngredient>> loadIngredientsByParticipantId(Long slotId) {
        List<SessionIngredient> ingredients = sessionIngredientRepository.findBySlotIdOrderByIdAsc(slotId);
        Map<Long, List<SessionIngredient>> ingredientsByParticipantId = new LinkedHashMap<>();
        for (SessionIngredient ingredient : ingredients) {
            ingredientsByParticipantId
                    .computeIfAbsent(ingredient.getParticipant().getId(), ignored -> new java.util.ArrayList<>())
                    .add(ingredient);
        }
        return ingredientsByParticipantId;
    }

    private List<SharedIngredientPoolItemResponse> buildSharedIngredientPool(
            Map<Long, List<SessionIngredient>> ingredientsByParticipantId
    ) {
        Map<Long, SharedIngredientAccumulator> accumulatorByIngredientId = new LinkedHashMap<>();
        ingredientsByParticipantId.values().stream()
                .flatMap(List::stream)
                .forEach(ingredient -> accumulatorByIngredientId
                        .computeIfAbsent(
                                ingredient.getIngredient().getId(),
                                ignored -> new SharedIngredientAccumulator(
                                        ingredient.getIngredient().getId(),
                                        ingredient.getIngredient().getNameKo()
                                )
                        )
                        .add(ingredient.getEstimatedGrams()));

        return accumulatorByIngredientId.values().stream()
                .sorted(Comparator.comparing(SharedIngredientAccumulator::ingredientId))
                .map(SharedIngredientAccumulator::toResponse)
                .toList();
    }

    private boolean canRequestRecommendation(
            Slot slot,
            List<SessionParticipant> participants,
            Map<Long, List<SessionIngredient>> ingredientsByParticipantId
    ) {
        return slot.getStatus() == SlotStatus.OPEN
                && participants.size() == slot.getCapacity()
                && participants.stream()
                .allMatch(participant -> !ingredientsByParticipantId
                        .getOrDefault(participant.getId(), List.of())
                .isEmpty());
    }

    private MenuCandidateResponse readSelectedMenu(String selectedMenuJson) {
        if (selectedMenuJson == null || selectedMenuJson.trim().isEmpty()) {
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

    private String refundHint(String menuType) {
        if ("LOW_CARBON".equals(menuType)) {
            return "저탄소 메뉴 선택으로 환급 점수 40점이 반영됩니다.";
        }
        return "소진량과 완성 음식 소비율에 따라 환급 점수가 반영됩니다.";
    }

    private static class SharedIngredientAccumulator {

        private final Long ingredientId;
        private final String nameKo;
        private BigDecimal estimatedTotalGrams = BigDecimal.ZERO;

        private SharedIngredientAccumulator(Long ingredientId, String nameKo) {
            this.ingredientId = ingredientId;
            this.nameKo = nameKo;
        }

        private Long ingredientId() {
            return ingredientId;
        }

        private void add(BigDecimal grams) {
            estimatedTotalGrams = estimatedTotalGrams.add(grams);
        }

        private SharedIngredientPoolItemResponse toResponse() {
            return new SharedIngredientPoolItemResponse(ingredientId, nameKo, estimatedTotalGrams);
        }
    }
}
