package com.kirozero.netzero.domain.slot.dto;

import com.kirozero.netzero.domain.slot.entity.Slot;
import com.kirozero.netzero.domain.slot.enums.SlotStatus;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public record SlotDetailResponse(
        Long slotId,
        LocalDate date,
        String placeName,
        String stationCode,
        LocalTime startTime,
        LocalTime endTime,
        int capacity,
        long participantCount,
        SlotStatus status,
        List<String> commonKit,
        boolean joined,
        Long myParticipantId,
        List<SlotDetailParticipantResponse> participants
) {

    public static SlotDetailResponse from(
            Slot slot,
            long participantCount,
            List<String> commonKit,
            boolean joined,
            Long myParticipantId,
            List<SlotDetailParticipantResponse> participants
    ) {
        return new SlotDetailResponse(
                slot.getId(),
                slot.getDate(),
                slot.getPlaceName(),
                slot.getStationCode(),
                slot.getStartTime(),
                slot.getEndTime(),
                slot.getCapacity(),
                participantCount,
                slot.getStatus(),
                commonKit,
                joined,
                myParticipantId,
                participants
        );
    }
}
