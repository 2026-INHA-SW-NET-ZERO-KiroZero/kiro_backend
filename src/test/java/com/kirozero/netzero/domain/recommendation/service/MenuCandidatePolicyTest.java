package com.kirozero.netzero.domain.recommendation.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.kirozero.netzero.domain.ai.model.AiIngredient;
import com.kirozero.netzero.domain.ai.model.RawMenuCandidate;
import com.kirozero.netzero.domain.recommendation.dto.CandidateUsedIngredientResponse;
import com.kirozero.netzero.domain.recommendation.dto.PurchaseItemResponse;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class MenuCandidatePolicyTest {

    @Test
    void enrichesPurchaseAllergenTagsFromKnownPurchaseName() {
        RawMenuCandidate normalized = MenuCandidatePolicy.normalizeCandidate(
                candidate("GENERAL", List.of(purchase("새우", "OTHER", List.of()))),
                Map.of()
        );

        PurchaseItemResponse purchase = normalized.purchaseItems().getFirst();
        assertEquals("SEAFOOD", purchase.category());
        assertTrue(purchase.allergenTags().contains("crustacean_shellfish"));
    }

    @Test
    void detectsBlockedAnimalPurchaseInLowCarbonCandidateEvenWhenClaudeCategoryIsOther() {
        RawMenuCandidate normalized = MenuCandidatePolicy.normalizeCandidate(
                candidate("LOW_CARBON", List.of(purchase("닭가슴살", "OTHER", List.of()))),
                Map.of()
        );

        assertTrue(MenuCandidatePolicy.hasBlockedLowCarbonPurchase(normalized));
    }

    @Test
    void allowsEggPurchaseInLowCarbonCandidateBecauseItIsNotMeatOrSeafood() {
        RawMenuCandidate normalized = MenuCandidatePolicy.normalizeCandidate(
                candidate("LOW_CARBON", List.of(purchase("계란", "OTHER", List.of()))),
                Map.of()
        );

        assertFalse(MenuCandidatePolicy.hasBlockedLowCarbonPurchase(normalized));
        assertEquals("EGG", normalized.purchaseItems().getFirst().category());
        assertTrue(normalized.purchaseItems().getFirst().allergenTags().contains("egg"));
    }

    @Test
    void clampsUsedIngredientGramsToSharedPoolAndRecalculatesRatio() {
        RawMenuCandidate normalized = MenuCandidatePolicy.normalizeCandidate(
                new RawMenuCandidate(
                        "양배추 볶음",
                        "GENERAL",
                        List.of(new CandidateUsedIngredientResponse(
                                12L,
                                "양배추",
                                new BigDecimal("999"),
                                new BigDecimal("500"),
                                new BigDecimal("0.10")
                        )),
                        List.of("식용유"),
                        List.of(),
                        30,
                        "LOW",
                        "테스트",
                        List.of("손질"),
                        List.of("조리")
                ),
                Map.of(12L, new AiIngredient(12L, "양배추", new BigDecimal("350")))
        );

        CandidateUsedIngredientResponse used = normalized.usedLeftoverIngredients().getFirst();
        assertEquals(0, new BigDecimal("350").compareTo(used.availableGrams()));
        assertEquals(0, new BigDecimal("350").compareTo(used.plannedUseGrams()));
        assertEquals(0, BigDecimal.ONE.compareTo(used.estimatedUseRatio()));
    }

    private RawMenuCandidate candidate(String menuType, List<PurchaseItemResponse> purchaseItems) {
        return new RawMenuCandidate(
                "테스트 메뉴",
                menuType,
                List.of(),
                List.of("식용유"),
                purchaseItems,
                30,
                "LOW",
                "테스트",
                List.of("손질"),
                List.of("조리")
        );
    }

    private PurchaseItemResponse purchase(String name, String category, List<String> allergenTags) {
        return new PurchaseItemResponse(
                name,
                category,
                new BigDecimal("100"),
                allergenTags,
                null,
                1000
        );
    }
}
