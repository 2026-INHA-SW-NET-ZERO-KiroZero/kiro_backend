package com.kirozero.netzero.domain.session.controller;

import com.kirozero.netzero.domain.session.dto.MySessionDetailResponse;
import com.kirozero.netzero.domain.session.dto.MySessionListResponse;
import com.kirozero.netzero.domain.session.service.MySessionService;
import com.kirozero.netzero.global.config.OpenApiConfig;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/me/sessions")
@RequiredArgsConstructor
public class MySessionController {

    private final MySessionService mySessionService;

    @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
    @GetMapping
    public MySessionListResponse getMySessions(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization
    ) {
        return mySessionService.getMySessions(authorization);
    }

    @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
    @GetMapping("/{slotId}")
    public MySessionDetailResponse getMySession(
            @PathVariable Long slotId,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization
    ) {
        return mySessionService.getMySession(slotId, authorization);
    }
}
