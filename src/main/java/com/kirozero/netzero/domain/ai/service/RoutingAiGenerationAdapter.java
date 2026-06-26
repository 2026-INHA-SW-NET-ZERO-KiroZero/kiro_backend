package com.kirozero.netzero.domain.ai.service;

import com.kirozero.netzero.domain.ai.config.AiGenerationProperties;
import com.kirozero.netzero.domain.ai.enums.AiProvider;
import com.kirozero.netzero.domain.ai.exception.AiGenerationException;
import com.kirozero.netzero.domain.ai.model.CookingGuideGenerationContext;
import com.kirozero.netzero.domain.ai.model.MenuCandidateGenerationContext;
import com.kirozero.netzero.domain.ai.model.RawMenuCandidate;
import com.kirozero.netzero.domain.ai.port.AiGenerationAdapter;
import com.kirozero.netzero.domain.ai.port.AiGenerationPort;
import com.kirozero.netzero.domain.cooking.dto.CookingGuideResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

@Primary
@Service
@RequiredArgsConstructor
public class RoutingAiGenerationAdapter implements AiGenerationPort {

    private static final Logger log = LoggerFactory.getLogger(RoutingAiGenerationAdapter.class);

    private final AiGenerationProperties properties;
    private final List<AiGenerationAdapter> adapters;

    @Override
    public List<RawMenuCandidate> generateMenuCandidates(MenuCandidateGenerationContext context) {
        return routeOrFallback(properties.getProvider()).generateMenuCandidates(context);
    }

    @Override
    public CookingGuideResponse generateCookingGuide(CookingGuideGenerationContext context) {
        return routeOrFallback(properties.getProvider()).generateCookingGuide(context);
    }

    private AiGenerationAdapter routeOrFallback(AiProvider provider) {
        AiGenerationAdapter selected = findAdapter(provider);
        if (selected != null) {
            return new FallbackAwareAdapter(selected, findRequiredAdapter(AiProvider.STUB), properties.isFallbackEnabled());
        }
        if (properties.isFallbackEnabled()) {
            return findRequiredAdapter(AiProvider.STUB);
        }
        throw new AiGenerationException("AI provider is not configured: " + provider);
    }

    private AiGenerationAdapter findRequiredAdapter(AiProvider provider) {
        AiGenerationAdapter adapter = findAdapter(provider);
        if (adapter == null) {
            throw new AiGenerationException("AI adapter is missing: " + provider);
        }
        return adapter;
    }

    private AiGenerationAdapter findAdapter(AiProvider provider) {
        return adapters.stream()
                .filter(adapter -> adapter.provider() == provider)
                .findFirst()
                .orElse(null);
    }

    private record FallbackAwareAdapter(
            AiGenerationAdapter selected,
            AiGenerationAdapter fallback,
            boolean fallbackEnabled
    ) implements AiGenerationAdapter {

        @Override
        public AiProvider provider() {
            return selected.provider();
        }

        @Override
        public List<RawMenuCandidate> generateMenuCandidates(MenuCandidateGenerationContext context) {
            try {
                return selected.generateMenuCandidates(context);
            } catch (RuntimeException e) {
                if (!fallbackEnabled || selected.provider() == AiProvider.STUB) {
                    throw e;
                }
                log.warn("AI menu generation failed with provider {}. Falling back to STUB. cause={}",
                        selected.provider(),
                        e.getMessage()
                );
                return fallback.generateMenuCandidates(context);
            }
        }

        @Override
        public CookingGuideResponse generateCookingGuide(CookingGuideGenerationContext context) {
            try {
                return selected.generateCookingGuide(context);
            } catch (RuntimeException e) {
                if (!fallbackEnabled || selected.provider() == AiProvider.STUB) {
                    throw e;
                }
                log.warn("AI cooking guide generation failed with provider {}. Falling back to STUB. cause={}",
                        selected.provider(),
                        e.getMessage()
                );
                return fallback.generateCookingGuide(context);
            }
        }
    }
}
