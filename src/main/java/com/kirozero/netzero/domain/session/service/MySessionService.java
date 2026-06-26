package com.kirozero.netzero.domain.session.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kirozero.netzero.domain.auth.service.AuthService;
import com.kirozero.netzero.domain.recommendation.dto.MenuCandidateResponse;
import com.kirozero.netzero.domain.result.repository.ConsumptionRecordRepository;
import com.kirozero.netzero.domain.session.dto.MySessionDetailResponse;
import com.kirozero.netzero.domain.session.dto.MySessionItemResponse;
import com.kirozero.netzero.domain.session.dto.MySessionListResponse;
import com.kirozero.netzero.domain.session.dto.SessionIngredientResponse;
import com.kirozero.netzero.domain.session.entity.SessionParticipant;
import com.kirozero.netzero.domain.session.repository.SessionIngredientRepository;
import com.kirozero.netzero.domain.session.repository.SessionParticipantRepository;
import com.kirozero.netzero.domain.slot.entity.Slot;
import com.kirozero.netzero.domain.slot.enums.SlotStatus;
import com.kirozero.netzero.domain.slot.repository.SlotRepository;
import com.kirozero.netzero.domain.user.entity.User;
import com.kirozero.netzero.domain.vote.dto.SelectedMenuSummaryResponse;
import java.time.format.DateTimeFormatter;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class MySessionService {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    private final AuthService authService;
    private final SlotRepository slotRepository;
    private final SessionParticipantRepository sessionParticipantRepository;
    private final SessionIngredientRepository sessionIngredientRepository;
    private final ConsumptionRecordRepository consumptionRecordRepository;
    private final SessionQueryService sessionQueryService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Transactional(readOnly = true)
    public MySessionListResponse getMySessions(String authorizationHeader) {
        User user = authService.requireUser(authorizationHeader);
        List<SessionParticipant> participants = sessionParticipantRepository.findByUserIdOrderByJoinedAtDesc(user.getId());

        return new MySessionListResponse(
                participants.stream()
                        .map(this::toItemResponse)
                        .toList()
        );
    }

    @Transactional(readOnly = true)
    public MySessionDetailResponse getMySession(Long slotId, String authorizationHeader) {
        User user = authService.requireUser(authorizationHeader);
        Slot slot = slotRepository.findById(slotId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Slot not found."));
        SessionParticipant participant = sessionParticipantRepository.findBySlotIdAndUserId(slotId, user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Only participants can view this session."));

        return new MySessionDetailResponse(
                slot.getId(),
                participant.getId(),
                true,
                participant.isCanPurchase(),
                slot.getStatus(),
                sessionIngredientRepository.findByParticipantIdOrderByIdAsc(participant.getId()).stream()
                        .map(SessionIngredientResponse::from)
                        .toList(),
                sessionQueryService.getSessionStatus(slotId),
                hasRecommendation(slot),
                hasSelectedMenu(slot),
                isCompleted(slot),
                selectedMenuSummary(slot)
        );
    }

    private MySessionItemResponse toItemResponse(SessionParticipant participant) {
        Slot slot = participant.getSlot();
        return new MySessionItemResponse(
                slot.getId(),
                participant.getId(),
                slot.getDate().toString(),
                slot.getPlaceName(),
                slot.getStationCode(),
                slot.getStartTime().format(TIME_FORMATTER),
                slot.getEndTime().format(TIME_FORMATTER),
                slot.getStartTime().format(TIME_FORMATTER) + "-" + slot.getEndTime().format(TIME_FORMATTER),
                slot.getCapacity(),
                sessionParticipantRepository.countBySlotId(slot.getId()),
                slot.getStatus(),
                participant.isCanPurchase(),
                sessionIngredientRepository.countByParticipantId(participant.getId()),
                hasRecommendation(slot),
                hasSelectedMenu(slot),
                isCompleted(slot),
                selectedMenuSummary(slot)
        );
    }

    private boolean hasRecommendation(Slot slot) {
        return StringUtils.hasText(slot.getCandidatesJson());
    }

    private boolean hasSelectedMenu(Slot slot) {
        return StringUtils.hasText(slot.getSelectedMenuJson());
    }

    private boolean isCompleted(Slot slot) {
        return slot.getStatus() == SlotStatus.COMPLETED || consumptionRecordRepository.existsBySlotId(slot.getId());
    }

    private SelectedMenuSummaryResponse selectedMenuSummary(Slot slot) {
        if (!hasSelectedMenu(slot)) {
            return null;
        }

        try {
            MenuCandidateResponse selectedMenu = objectMapper.readValue(
                    unwrapStoredJson(slot.getSelectedMenuJson()),
                    MenuCandidateResponse.class
            );
            return new SelectedMenuSummaryResponse(
                    selectedMenu.candidateLabel(),
                    selectedMenu.menuName(),
                    selectedMenu.menuType()
            );
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
