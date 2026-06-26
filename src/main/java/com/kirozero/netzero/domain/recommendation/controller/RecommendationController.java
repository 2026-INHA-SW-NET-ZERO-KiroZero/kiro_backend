package com.kirozero.netzero.domain.recommendation.controller;

import com.kirozero.netzero.domain.recommendation.dto.RecommendationRequest;
import com.kirozero.netzero.domain.recommendation.dto.RecommendationResponse;
import com.kirozero.netzero.domain.recommendation.service.RecommendationService;
import com.kirozero.netzero.global.config.OpenApiConfig;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
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
public class RecommendationController {

    private final RecommendationService recommendationService;

    @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
    @PostMapping("/{slotId}/recommendations")
    public RecommendationResponse recommend(
            @PathVariable Long slotId,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @RequestBody(required = false) RecommendationRequest request
    ) {
        RecommendationRequest safeRequest = request == null ? new RecommendationRequest("INITIAL") : request;
        return recommendationService.recommend(slotId, authorization, safeRequest);
    }
}
