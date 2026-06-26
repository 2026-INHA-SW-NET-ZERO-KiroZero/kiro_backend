package com.kirozero.netzero.domain.ai.port;

import com.kirozero.netzero.domain.ai.model.CookingGuideGenerationContext;
import com.kirozero.netzero.domain.ai.model.MenuCandidateGenerationContext;
import com.kirozero.netzero.domain.ai.model.RawMenuCandidate;
import com.kirozero.netzero.domain.cooking.dto.CookingGuideResponse;
import java.util.List;

public interface AiGenerationPort {

    /**
     * USER_FLOW 4.8에 따라 일반식 3개 + 저탄소 3개, 총 6개의 raw 후보를 생성한다.
     * 최종 4개 선정과 candidateLabel(A/B/C/D) 부여, 정책 검증은 호출자(백엔드)가 수행한다.
     */
    List<RawMenuCandidate> generateMenuCandidates(MenuCandidateGenerationContext context);

    CookingGuideResponse generateCookingGuide(CookingGuideGenerationContext context);
}
