package com.kirozero.netzero.domain.ai.model;

import com.kirozero.netzero.domain.recommendation.dto.CandidateUsedIngredientResponse;
import com.kirozero.netzero.domain.recommendation.dto.PurchaseItemResponse;
import java.util.List;

/**
 * LLM이 한 차례 호출에서 반환한 후보 6개 중 하나를 백엔드 검증/선정 단계로 넘기는 내부 표현.
 * 클라이언트에 노출되는 {@link com.kirozero.netzero.domain.recommendation.dto.MenuCandidateResponse}
 * 와 달리 candidateLabel은 아직 부여하지 않는다(백엔드가 선정 후 부여).
 */
public record RawMenuCandidate(
        String menuName,
        String menuType,
        List<CandidateUsedIngredientResponse> usedLeftoverIngredients,
        List<String> commonKitItems,
        List<PurchaseItemResponse> purchaseItems,
        int cookingTimeMinutes,
        String difficulty,
        String recommendationReason,
        List<String> cookingOutlineSteps,
        List<String> rolePlanSummary
) {
}
