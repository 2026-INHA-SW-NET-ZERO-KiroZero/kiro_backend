package com.kirozero.netzero.domain.slot.service;

import com.kirozero.netzero.domain.slot.dto.SlotListItemResponse;
import com.kirozero.netzero.domain.slot.dto.SlotListResponse;
import com.kirozero.netzero.domain.slot.entity.Slot;
import com.kirozero.netzero.domain.slot.enums.SlotStatus;
import com.kirozero.netzero.domain.slot.repository.SlotRepository;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SlotService {

    private static final List<String> COMMON_KIT_SUMMARY = List.of("식용유", "간장", "소금", "후추");

    private final SlotRepository slotRepository;

    @Transactional(readOnly = true)
    public SlotListResponse getSlots(LocalDate date, SlotStatus status) {
        List<Slot> slots = status == null
                ? slotRepository.findByDateOrderByStartTimeAscStationCodeAsc(date)
                : slotRepository.findByDateAndStatusOrderByStartTimeAscStationCodeAsc(date, status);

        return new SlotListResponse(
                slots.stream()
                        .map(slot -> SlotListItemResponse.from(slot, 0, COMMON_KIT_SUMMARY))
                        .toList()
        );
    }
}
