package com.kirozero.netzero.domain.recommendation.service;

import com.kirozero.netzero.domain.ai.model.AiIngredient;
import com.kirozero.netzero.domain.ai.model.RawMenuCandidate;
import com.kirozero.netzero.domain.recommendation.dto.CandidateUsedIngredientResponse;
import com.kirozero.netzero.domain.recommendation.dto.PurchaseItemResponse;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.util.StringUtils;

final class MenuCandidatePolicy {

    private static final Set<String> LOW_CARBON_BLOCKED_PURCHASE_CATEGORIES = Set.of(
            "MEAT",
            "SEAFOOD",
            "POULTRY",
            "FISH",
            "BEEF",
            "PORK",
            "CHICKEN",
            "SHELLFISH"
    );

    private static final Map<String, PurchasePolicy> PURCHASE_POLICIES = purchasePolicies();

    private MenuCandidatePolicy() {
    }

    static RawMenuCandidate normalizeCandidate(
            RawMenuCandidate candidate,
            Map<Long, AiIngredient> sharedPoolByIngredientId
    ) {
        return new RawMenuCandidate(
                candidate.menuName(),
                normalizeText(candidate.menuType()),
                normalizeUsedIngredients(candidate.usedLeftoverIngredients(), sharedPoolByIngredientId),
                safeList(candidate.commonKitItems()),
                normalizePurchaseItems(candidate.purchaseItems()),
                candidate.cookingTimeMinutes(),
                candidate.difficulty(),
                candidate.recommendationReason(),
                safeList(candidate.cookingOutlineSteps()),
                safeList(candidate.rolePlanSummary())
        );
    }

    static boolean hasBlockedLowCarbonPurchase(RawMenuCandidate candidate) {
        if (!"LOW_CARBON".equals(candidate.menuType())) {
            return false;
        }
        for (PurchaseItemResponse purchase : candidate.purchaseItems()) {
            String category = normalizeText(purchase.category());
            if (LOW_CARBON_BLOCKED_PURCHASE_CATEGORIES.contains(category)) {
                return true;
            }
        }
        return false;
    }

    private static List<CandidateUsedIngredientResponse> normalizeUsedIngredients(
            List<CandidateUsedIngredientResponse> usedIngredients,
            Map<Long, AiIngredient> sharedPoolByIngredientId
    ) {
        List<CandidateUsedIngredientResponse> normalized = new ArrayList<>();
        for (CandidateUsedIngredientResponse used : safeList(usedIngredients)) {
            AiIngredient shared = sharedPoolByIngredientId.get(used.ingredientId());
            if (shared == null) {
                normalized.add(used);
                continue;
            }

            BigDecimal availableGrams = nonNegative(shared.availableGrams());
            BigDecimal plannedUseGrams = clamp(nonNegative(used.plannedUseGrams()), BigDecimal.ZERO, availableGrams);
            BigDecimal estimatedUseRatio = calculateUseRatio(availableGrams, plannedUseGrams);
            normalized.add(new CandidateUsedIngredientResponse(
                    shared.ingredientId(),
                    shared.nameKo(),
                    availableGrams,
                    plannedUseGrams,
                    estimatedUseRatio
            ));
        }
        return normalized;
    }

    private static List<PurchaseItemResponse> normalizePurchaseItems(List<PurchaseItemResponse> purchaseItems) {
        List<PurchaseItemResponse> normalized = new ArrayList<>();
        for (PurchaseItemResponse item : safeList(purchaseItems)) {
            PurchasePolicy policy = PURCHASE_POLICIES.get(normalizeNameKey(item.name()));
            Set<String> allergenTags = new LinkedHashSet<>();
            for (String tag : safeList(item.allergenTags())) {
                if (StringUtils.hasText(tag)) {
                    allergenTags.add(tag.trim().toLowerCase(Locale.ROOT));
                }
            }
            if (policy != null) {
                allergenTags.addAll(policy.allergenTags());
            }

            normalized.add(new PurchaseItemResponse(
                    item.name(),
                    policy == null ? normalizeText(item.category()) : policy.category(),
                    item.quantityGrams(),
                    List.copyOf(allergenTags),
                    item.assignedToNickname(),
                    item.estimatedCost()
            ));
        }
        return normalized;
    }

    private static Map<String, PurchasePolicy> purchasePolicies() {
        Map<String, PurchasePolicy> policies = new LinkedHashMap<>();
        add(policies, "계란", "EGG", "egg");
        add(policies, "달걀", "EGG", "egg");
        add(policies, "마요네즈", "EGG", "egg");
        add(policies, "우유", "DAIRY", "milk");
        add(policies, "치즈", "DAIRY", "milk");
        add(policies, "버터", "DAIRY", "milk");
        add(policies, "두부", "SOY", "soy");
        add(policies, "순두부", "SOY", "soy");
        add(policies, "콩나물", "SOY", "soy");
        add(policies, "새우", "SEAFOOD", "crustacean_shellfish");
        add(policies, "게", "SEAFOOD", "crustacean_shellfish");
        add(policies, "꽃게", "SEAFOOD", "crustacean_shellfish");
        add(policies, "랍스터", "SEAFOOD", "crustacean_shellfish");
        add(policies, "오징어", "SEAFOOD", "mollusk_shellfish");
        add(policies, "조개", "SEAFOOD", "mollusk_shellfish");
        add(policies, "바지락", "SEAFOOD", "mollusk_shellfish");
        add(policies, "홍합", "SEAFOOD", "mollusk_shellfish");
        add(policies, "고등어", "FISH", "fish");
        add(policies, "연어", "FISH", "fish");
        add(policies, "참치", "FISH", "fish");
        add(policies, "어묵", "FISH", "fish", "wheat");
        add(policies, "닭가슴살", "POULTRY");
        add(policies, "닭고기", "POULTRY");
        add(policies, "닭다리살", "POULTRY");
        add(policies, "돼지고기", "MEAT");
        add(policies, "삼겹살", "MEAT");
        add(policies, "소고기", "MEAT");
        add(policies, "베이컨", "MEAT");
        add(policies, "소시지", "MEAT");
        add(policies, "밀가루", "WHEAT", "wheat");
        add(policies, "부침가루", "WHEAT", "wheat");
        return Map.copyOf(policies);
    }

    private static void add(Map<String, PurchasePolicy> policies, String name, String category, String... allergenTags) {
        policies.put(normalizeNameKey(name), new PurchasePolicy(category, List.of(allergenTags)));
    }

    private static BigDecimal calculateUseRatio(BigDecimal availableGrams, BigDecimal plannedUseGrams) {
        if (availableGrams.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        return plannedUseGrams.divide(availableGrams, 4, RoundingMode.HALF_UP);
    }

    private static BigDecimal nonNegative(BigDecimal value) {
        if (value == null || value.compareTo(BigDecimal.ZERO) < 0) {
            return BigDecimal.ZERO;
        }
        return value;
    }

    private static BigDecimal clamp(BigDecimal value, BigDecimal min, BigDecimal max) {
        if (value.compareTo(min) < 0) {
            return min;
        }
        if (value.compareTo(max) > 0) {
            return max;
        }
        return value;
    }

    private static String normalizeText(String value) {
        if (!StringUtils.hasText(value)) {
            return "OTHER";
        }
        return value.trim().toUpperCase(Locale.ROOT);
    }

    private static String normalizeNameKey(String name) {
        if (!StringUtils.hasText(name)) {
            return "";
        }
        return name.replaceAll("\\s+", "").toLowerCase(Locale.ROOT);
    }

    private static <T> List<T> safeList(List<T> value) {
        return value == null ? List.of() : value;
    }

    private record PurchasePolicy(String category, List<String> allergenTags) {
    }
}
