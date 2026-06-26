package com.kirozero.netzero.domain.vote.controller;

import com.kirozero.netzero.domain.vote.dto.MenuVoteRequest;
import com.kirozero.netzero.domain.vote.dto.MenuVoteResponse;
import com.kirozero.netzero.domain.vote.service.MenuVoteService;
import com.kirozero.netzero.global.config.OpenApiConfig;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/sessions")
@RequiredArgsConstructor
public class MenuVoteController {

    private final MenuVoteService menuVoteService;

    @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
    @PostMapping("/{slotId}/votes")
    public MenuVoteResponse vote(
            @PathVariable Long slotId,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @Valid @RequestBody MenuVoteRequest request
    ) {
        return menuVoteService.vote(slotId, authorization, request);
    }
}
