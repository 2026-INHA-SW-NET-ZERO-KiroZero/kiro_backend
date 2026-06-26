package com.kirozero.netzero.domain.slot.controller;

import com.kirozero.netzero.domain.slot.dto.SlotListResponse;
import com.kirozero.netzero.domain.slot.enums.SlotStatus;
import com.kirozero.netzero.domain.slot.service.SlotService;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/slots")
@RequiredArgsConstructor
public class SlotController {

    private final SlotService slotService;

    @GetMapping
    public SlotListResponse getSlots(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) SlotStatus status
    ) {
        return slotService.getSlots(date, status);
    }
}
