package com.kirozero.netzero.domain.ai.port;

import com.kirozero.netzero.domain.ai.model.CookingGuideGenerationContext;
import com.kirozero.netzero.domain.ai.model.MenuCandidateGenerationContext;
import com.kirozero.netzero.domain.cooking.dto.CookingGuideResponse;
import com.kirozero.netzero.domain.recommendation.dto.MenuCandidateResponse;
import java.util.List;

public interface AiGenerationPort {

    List<MenuCandidateResponse> generateMenuCandidates(MenuCandidateGenerationContext context);

    CookingGuideResponse generateCookingGuide(CookingGuideGenerationContext context);
}
