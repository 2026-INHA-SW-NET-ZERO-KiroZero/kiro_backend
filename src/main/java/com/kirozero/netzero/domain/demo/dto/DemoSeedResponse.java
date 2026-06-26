package com.kirozero.netzero.domain.demo.dto;

public record DemoSeedResponse(
        int createdIngredients,
        int createdSlots,
        int createdUsers,
        String message
) {
}
