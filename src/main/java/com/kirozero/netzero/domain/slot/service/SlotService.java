package com.kirozero.netzero.domain.slot.service;

import com.kirozero.netzero.domain.session.entity.SessionParticipant;
import com.kirozero.netzero.domain.session.repository.SessionParticipantRepository;
import com.kirozero.netzero.domain.slot.dto.SlotDetailParticipantResponse;
import com.kirozero.netzero.domain.slot.dto.SlotDetailResponse;
import com.kirozero.netzero.domain.slot.dto.SlotListItemResponse;
import com.kirozero.netzero.domain.slot.dto.SlotListResponse;
import com.kirozero.netzero.domain.slot.entity.Slot;
import com.kirozero.netzero.domain.slot.enums.SlotStatus;
import com.kirozero.netzero.domain.slot.repository.SlotRepository;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class SlotService {

    private static final List<String> COMMON_KIT = List.of("식용유", "간장", "소금", "후추", "참기름");
    private static final List<String> COMMON_KIT_SUMMARY = COMMON_KIT.subList(0, 4);

    private final SlotRepository slotRepository;
    private final SessionParticipantRepository sessionParticipantRepository;

    @Transactional(readOnly = true)
    public SlotListResponse getSlots(LocalDate date, SlotStatus status) {
        List<Slot> slots = status == null
                ? slotRepository.findByDateOrderByStartTimeAscStationCodeAsc(date)
                : slotRepository.findByDateAndStatusOrderByStartTimeAscStationCodeAsc(date, status);

        return new SlotListResponse(
                slots.stream()
                        .map(slot -> SlotListItemResponse.from(
                                slot,
                                Math.toIntExact(sessionParticipantRepository.countBySlotId(slot.getId())),
                                COMMON_KIT_SUMMARY
                        ))
                        .toList()
        );
    }

    @Transactional(readOnly = true)
    public SlotDetailResponse getSlot(Long slotId) {
        Slot slot = slotRepository.findById(slotId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Slot not found."));
        List<SessionParticipant> participants = sessionParticipantRepository.findBySlotIdOrderByJoinedAtAsc(slotId);

        return SlotDetailResponse.from(
                slot,
                participants.size(),
                COMMON_KIT,
                participants.stream()
                        .map(participant -> SlotDetailParticipantResponse.from(participant, 0))
                        .toList()
        );
    }
}
