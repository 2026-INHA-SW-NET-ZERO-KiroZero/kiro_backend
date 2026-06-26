package com.kirozero.netzero.domain.session.dto;

import com.kirozero.netzero.domain.recommendation.dto.PurchaseItemResponse;
import java.util.List;

public record SessionChecklistResponse(
        Long slotId,
        String menuName,
        String menuType,
        List<SessionIngredientResponse> myIngredients,
        List<String> commonKitItems,
        List<PurchaseItemResponse> purchaseItems,
        int reservationCredit,
        String refundHint
) {
}
