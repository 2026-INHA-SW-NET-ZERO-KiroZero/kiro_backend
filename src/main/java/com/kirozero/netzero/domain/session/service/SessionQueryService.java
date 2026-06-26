package com.kirozero.netzero.domain.session.service;

import com.kirozero.netzero.domain.session.dto.SessionIngredientStatusResponse;
import com.kirozero.netzero.domain.session.dto.SessionParticipantStatusResponse;
import com.kirozero.netzero.domain.session.dto.SessionStatusResponse;
import com.kirozero.netzero.domain.session.dto.SharedIngredientPoolItemResponse;
import com.kirozero.netzero.domain.session.entity.SessionIngredient;
import com.kirozero.netzero.domain.session.entity.SessionParticipant;
import com.kirozero.netzero.domain.session.repository.SessionIngredientRepository;
import com.kirozero.netzero.domain.session.repository.SessionParticipantRepository;
import com.kirozero.netzero.domain.slot.entity.Slot;
import com.kirozero.netzero.domain.slot.repository.SlotRepository;
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

    private final SlotRepository slotRepository;
    private final SessionParticipantRepository sessionParticipantRepository;
    private final SessionIngredientRepository sessionIngredientRepository;

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
        return participants.size() == slot.getCapacity()
                && participants.stream()
                .allMatch(participant -> !ingredientsByParticipantId
                        .getOrDefault(participant.getId(), List.of())
                        .isEmpty());
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
