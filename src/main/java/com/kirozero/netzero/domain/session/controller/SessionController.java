package com.kirozero.netzero.domain.session.controller;

import com.kirozero.netzero.domain.session.dto.SessionChecklistResponse;
import com.kirozero.netzero.domain.session.dto.SessionStatusResponse;
import com.kirozero.netzero.domain.session.dto.UpdateSessionIngredientsRequest;
import com.kirozero.netzero.domain.session.dto.UpdateSessionIngredientsResponse;
import com.kirozero.netzero.domain.session.service.SessionParticipationService;
import com.kirozero.netzero.domain.session.service.SessionQueryService;
import com.kirozero.netzero.global.config.OpenApiConfig;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/sessions")
@RequiredArgsConstructor
public class SessionController {

    private final SessionParticipationService sessionParticipationService;
    private final SessionQueryService sessionQueryService;

    @GetMapping("/{slotId}")
    public SessionStatusResponse getSessionStatus(@PathVariable Long slotId) {
        return sessionQueryService.getSessionStatus(slotId);
    }

    @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
    @GetMapping("/{slotId}/checklist")
    public SessionChecklistResponse getChecklist(
            @PathVariable Long slotId,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization
    ) {
        return sessionQueryService.getChecklist(slotId, authorization);
    }

    @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
    @PutMapping("/{slotId}/ingredients")
    public UpdateSessionIngredientsResponse updateIngredients(
            @PathVariable Long slotId,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @Valid @RequestBody UpdateSessionIngredientsRequest request
    ) {
        return sessionParticipationService.updateIngredients(slotId, authorization, request);
    }
}
