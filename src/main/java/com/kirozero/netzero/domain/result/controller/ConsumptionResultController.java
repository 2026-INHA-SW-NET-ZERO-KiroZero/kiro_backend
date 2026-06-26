package com.kirozero.netzero.domain.result.controller;

import com.kirozero.netzero.domain.result.dto.CreateConsumptionRecordRequest;
import com.kirozero.netzero.domain.result.dto.CreateConsumptionRecordResponse;
import com.kirozero.netzero.domain.result.dto.MyResultTotalResponse;
import com.kirozero.netzero.domain.result.dto.SessionResultResponse;
import com.kirozero.netzero.domain.result.service.ConsumptionResultService;
import com.kirozero.netzero.global.config.OpenApiConfig;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ConsumptionResultController {

    private final ConsumptionResultService consumptionResultService;

    @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
    @PostMapping("/sessions/{slotId}/consumption-records")
    public CreateConsumptionRecordResponse createRecord(
            @PathVariable Long slotId,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @Valid @RequestBody CreateConsumptionRecordRequest request
    ) {
        return consumptionResultService.createRecord(slotId, authorization, request);
    }

    @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
    @GetMapping("/sessions/{slotId}/result")
    public SessionResultResponse getSessionResult(
            @PathVariable Long slotId,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization
    ) {
        return consumptionResultService.getSessionResult(slotId, authorization);
    }

    @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
    @GetMapping("/me/results/total")
    public MyResultTotalResponse getMyResultTotal(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization
    ) {
        return consumptionResultService.getMyResultTotal(authorization);
    }
}
