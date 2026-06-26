package com.kirozero.netzero.domain.recommendation.dto;

import java.math.BigDecimal;
import java.util.List;

public record PurchaseItemResponse(
        String name,
        String category,
        BigDecimal quantityGrams,
        List<String> allergenTags,
        String assignedToNickname,
        int estimatedCost
) {
}
