package com.kirozero.netzero.domain.slot.dto;

import com.kirozero.netzero.domain.slot.entity.Slot;
import java.time.format.DateTimeFormatter;
import java.util.List;

public record SlotListItemResponse(
        Long slotId,
        String date,
        String placeName,
        String stationCode,
        String startTime,
        String endTime,
        int capacity,
        int participantCount,
        String status,
        List<String> commonKitSummary
) {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    public static SlotListItemResponse from(Slot slot, int participantCount, List<String> commonKitSummary) {
        return new SlotListItemResponse(
                slot.getId(),
                slot.getDate().toString(),
                slot.getPlaceName(),
                slot.getStationCode(),
                slot.getStartTime().format(TIME_FORMATTER),
                slot.getEndTime().format(TIME_FORMATTER),
                slot.getCapacity(),
                participantCount,
                slot.getStatus().name(),
                commonKitSummary
        );
    }
}
