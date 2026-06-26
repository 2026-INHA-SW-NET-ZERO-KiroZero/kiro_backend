package com.kirozero.netzero.domain.slot.controller;

import com.kirozero.netzero.domain.session.dto.JoinSlotRequest;
import com.kirozero.netzero.domain.session.dto.JoinSlotResponse;
import com.kirozero.netzero.domain.session.service.SessionParticipationService;
import com.kirozero.netzero.domain.slot.dto.SlotDetailResponse;
import com.kirozero.netzero.domain.slot.dto.SlotListResponse;
import com.kirozero.netzero.domain.slot.enums.SlotStatus;
import com.kirozero.netzero.domain.slot.service.SlotService;
import com.kirozero.netzero.global.config.OpenApiConfig;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/slots")
@RequiredArgsConstructor
public class SlotController {

    private final SlotService slotService;
    private final SessionParticipationService sessionParticipationService;

    @GetMapping
    public SlotListResponse getSlots(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) SlotStatus status
    ) {
        return slotService.getSlots(date, status);
    }

    @GetMapping("/{slotId}")
    public SlotDetailResponse getSlot(@PathVariable Long slotId) {
        return slotService.getSlot(slotId);
    }

    @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
    @PostMapping("/{slotId}/join")
    public JoinSlotResponse joinSlot(
            @PathVariable Long slotId,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @Valid @RequestBody JoinSlotRequest request
    ) {
        return sessionParticipationService.joinSlot(slotId, authorization, request);
    }
}
