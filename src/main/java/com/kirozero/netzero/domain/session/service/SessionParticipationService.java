package com.kirozero.netzero.domain.session.service;

import com.kirozero.netzero.domain.auth.service.AuthService;
import com.kirozero.netzero.domain.ingredient.entity.IngredientMaster;
import com.kirozero.netzero.domain.ingredient.repository.IngredientMasterRepository;
import com.kirozero.netzero.domain.session.dto.JoinIngredientRequest;
import com.kirozero.netzero.domain.session.dto.JoinSlotRequest;
import com.kirozero.netzero.domain.session.dto.JoinSlotResponse;
import com.kirozero.netzero.domain.session.dto.SessionIngredientResponse;
import com.kirozero.netzero.domain.session.entity.SessionIngredient;
import com.kirozero.netzero.domain.session.entity.SessionParticipant;
import com.kirozero.netzero.domain.session.repository.SessionIngredientRepository;
import com.kirozero.netzero.domain.session.repository.SessionParticipantRepository;
import com.kirozero.netzero.domain.slot.entity.Slot;
import com.kirozero.netzero.domain.slot.enums.SlotStatus;
import com.kirozero.netzero.domain.slot.repository.SlotRepository;
import com.kirozero.netzero.domain.user.entity.User;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class SessionParticipationService {

    private final AuthService authService;
    private final SlotRepository slotRepository;
    private final IngredientMasterRepository ingredientMasterRepository;
    private final SessionParticipantRepository sessionParticipantRepository;
    private final SessionIngredientRepository sessionIngredientRepository;

    @Transactional
    public JoinSlotResponse joinSlot(Long slotId, String authorizationHeader, JoinSlotRequest request) {
        User user = authService.requireUser(authorizationHeader);
        Slot slot = slotRepository.findById(slotId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Slot not found."));

        validateJoinable(slot, user);

        SessionParticipant participant = sessionParticipantRepository.save(
                SessionParticipant.create(slot, user, request.canPurchase())
        );
        List<SessionIngredient> ingredients = sessionIngredientRepository.saveAll(
                createSessionIngredients(slot, participant, request.ingredients())
        );

        return new JoinSlotResponse(
                slot.getId(),
                participant.getId(),
                slot.getStatus(),
                participant.isCanPurchase(),
                ingredients.stream()
                        .map(SessionIngredientResponse::from)
                        .toList()
        );
    }

    private void validateJoinable(Slot slot, User user) {
        if (slot.getStatus() != SlotStatus.OPEN) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Only OPEN slots can be joined.");
        }

        if (sessionParticipantRepository.existsBySlotIdAndUserId(slot.getId(), user.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Already joined this slot.");
        }

        long participantCount = sessionParticipantRepository.countBySlotId(slot.getId());
        if (participantCount >= slot.getCapacity()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Slot capacity is full.");
        }
    }

    private List<SessionIngredient> createSessionIngredients(
            Slot slot,
            SessionParticipant participant,
            List<JoinIngredientRequest> requests
    ) {
        Map<Long, IngredientMaster> ingredientsById = ingredientMasterRepository.findAllById(
                        requests.stream()
                                .map(JoinIngredientRequest::ingredientId)
                                .toList()
                ).stream()
                .collect(Collectors.toMap(
                        IngredientMaster::getId,
                        Function.identity(),
                        (left, right) -> left,
                        LinkedHashMap::new
                ));

        return requests.stream()
                .map(request -> createSessionIngredient(slot, participant, request, ingredientsById))
                .toList();
    }

    private SessionIngredient createSessionIngredient(
            Slot slot,
            SessionParticipant participant,
            JoinIngredientRequest request,
            Map<Long, IngredientMaster> ingredientsById
    ) {
        IngredientMaster ingredient = ingredientsById.get(request.ingredientId());
        if (ingredient == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown ingredient: " + request.ingredientId());
        }

        BigDecimal estimatedGrams = request.knownGrams() == null
                ? ingredient.getGramsPerCount().multiply(request.count())
                : request.knownGrams();

        return SessionIngredient.create(
                slot,
                participant,
                ingredient,
                request.count(),
                request.knownGrams(),
                estimatedGrams
        );
    }
}
