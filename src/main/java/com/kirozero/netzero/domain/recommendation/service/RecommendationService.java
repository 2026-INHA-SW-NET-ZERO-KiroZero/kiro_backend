package com.kirozero.netzero.domain.recommendation.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kirozero.netzero.domain.auth.service.AuthService;
import com.kirozero.netzero.domain.recommendation.dto.CandidateUsedIngredientResponse;
import com.kirozero.netzero.domain.recommendation.dto.LatestRecommendationResponse;
import com.kirozero.netzero.domain.recommendation.dto.MenuCandidateResponse;
import com.kirozero.netzero.domain.recommendation.dto.PurchaseItemResponse;
import com.kirozero.netzero.domain.recommendation.dto.RecommendationRequest;
import com.kirozero.netzero.domain.recommendation.dto.RecommendationResponse;
import com.kirozero.netzero.domain.session.entity.SessionIngredient;
import com.kirozero.netzero.domain.session.entity.SessionParticipant;
import com.kirozero.netzero.domain.session.repository.SessionIngredientRepository;
import com.kirozero.netzero.domain.session.repository.SessionParticipantRepository;
import com.kirozero.netzero.domain.slot.entity.Slot;
import com.kirozero.netzero.domain.slot.enums.SlotStatus;
import com.kirozero.netzero.domain.slot.repository.SlotRepository;
import com.kirozero.netzero.domain.user.entity.User;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class RecommendationService {

    private static final BigDecimal DEFAULT_USE_RATIO = new BigDecimal("0.80");
    private static final List<String> COMMON_KIT = List.of("식용유", "간장", "소금", "후추", "참기름");

    private final AuthService authService;
    private final SlotRepository slotRepository;
    private final SessionParticipantRepository sessionParticipantRepository;
    private final SessionIngredientRepository sessionIngredientRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Transactional
    public RecommendationResponse recommend(Long slotId, String authorizationHeader, RecommendationRequest request) {
        User user = authService.requireUser(authorizationHeader);
        Slot slot = slotRepository.findById(slotId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Slot not found."));
        List<SessionParticipant> participants = sessionParticipantRepository.findBySlotIdOrderByJoinedAtAsc(slotId);
        List<SessionIngredient> ingredients = sessionIngredientRepository.findBySlotIdOrderByIdAsc(slotId);

        validateRecommendationRequest(slot, user, participants, ingredients);

        List<IngredientPoolItem> poolItems = buildPoolItems(ingredients);
        List<MenuCandidateResponse> candidates = buildStubCandidates(poolItems, participants);
        slot.proposeMenuCandidates(writeCandidatesJson(candidates));

        return new RecommendationResponse(
                slot.getId(),
                slot.getRecommendationCount(),
                slot.getStatus(),
                candidates
        );
    }

    @Transactional(readOnly = true)
    public LatestRecommendationResponse getLatestRecommendation(Long slotId, String authorizationHeader) {
        User user = authService.requireUser(authorizationHeader);
        Slot slot = slotRepository.findById(slotId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Slot not found."));

        if (!sessionParticipantRepository.existsBySlotIdAndUserId(slotId, user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only session participants can view recommendations.");
        }

        return new LatestRecommendationResponse(
                slot.getId(),
                slot.getRecommendationCount(),
                slot.getStatus(),
                readCandidates(slot.getCandidatesJson()),
                readSelectedMenu(slot.getSelectedMenuJson())
        );
    }

    private void validateRecommendationRequest(
            Slot slot,
            User user,
            List<SessionParticipant> participants,
            List<SessionIngredient> ingredients
    ) {
        if (slot.getStatus() != SlotStatus.OPEN) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Recommendations can only be requested for OPEN slots.");
        }

        boolean participant = participants.stream()
                .anyMatch(item -> item.getUser().getId().equals(user.getId()));
        if (!participant) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only session participants can request recommendations.");
        }

        if (participants.size() != slot.getCapacity()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Slot must be full before recommendation.");
        }

        Map<Long, Long> ingredientCountByParticipantId = new LinkedHashMap<>();
        for (SessionIngredient ingredient : ingredients) {
            ingredientCountByParticipantId.merge(ingredient.getParticipant().getId(), 1L, Long::sum);
        }

        boolean allHaveIngredients = participants.stream()
                .allMatch(item -> ingredientCountByParticipantId.getOrDefault(item.getId(), 0L) > 0);
        if (!allHaveIngredients) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Every participant must have at least one ingredient.");
        }
    }

    private List<IngredientPoolItem> buildPoolItems(List<SessionIngredient> ingredients) {
        Map<Long, IngredientPoolItem> poolByIngredientId = new LinkedHashMap<>();
        for (SessionIngredient ingredient : ingredients) {
            poolByIngredientId
                    .computeIfAbsent(
                            ingredient.getIngredient().getId(),
                            ignored -> new IngredientPoolItem(
                                    ingredient.getIngredient().getId(),
                                    ingredient.getIngredient().getNameKo()
                            )
                    )
                    .add(ingredient.getEstimatedGrams());
        }

        return poolByIngredientId.values().stream()
                .sorted(Comparator.comparing(IngredientPoolItem::estimatedTotalGrams).reversed())
                .toList();
    }

    private List<MenuCandidateResponse> buildStubCandidates(
            List<IngredientPoolItem> poolItems,
            List<SessionParticipant> participants
    ) {
        List<CandidateUsedIngredientResponse> mainUses = toCandidateUses(poolItems);
        String purchaserNickname = participants.stream()
                .filter(SessionParticipant::isCanPurchase)
                .findFirst()
                .map(participant -> participant.getUser().getNickname())
                .orElse(null);

        return List.of(
                new MenuCandidateResponse(
                        "A",
                        "냉장고 채소 볶음밥",
                        "GENERAL",
                        mainUses,
                        List.of("식용유", "간장", "후추", "참기름"),
                        List.of(),
                        35,
                        "LOW",
                        "남은 재료를 잘게 썰어 한 번에 볶을 수 있어 소진율이 높습니다.",
                        List.of("재료를 비슷한 크기로 손질합니다.", "밥과 함께 볶고 간장으로 간을 맞춥니다.", "참기름으로 마무리합니다."),
                        List.of("손질", "볶기", "간 맞춤", "플레이팅")
                ),
                new MenuCandidateResponse(
                        "B",
                        "감자 채소전",
                        "GENERAL",
                        mainUses,
                        List.of("식용유", "소금", "후추"),
                        purchaseItems(purchaserNickname),
                        40,
                        "MEDIUM",
                        "채소와 전분감 있는 재료를 묶어 조리하기 쉬운 일반식 후보입니다.",
                        List.of("재료를 얇게 채 썹니다.", "반죽 농도를 맞춥니다.", "앞뒤로 노릇하게 굽습니다."),
                        List.of("채썰기", "반죽", "부치기", "정리")
                ),
                new MenuCandidateResponse(
                        "C",
                        "저탄소 채소 비빔밥",
                        "LOW_CARBON",
                        mainUses,
                        List.of("간장", "참기름", "소금"),
                        List.of(),
                        30,
                        "LOW",
                        "추가 고기류 없이 남은 채소 중심으로 구성해 저탄소 선택에 적합합니다.",
                        List.of("재료를 데치거나 볶습니다.", "밥 위에 재료를 올립니다.", "간장 양념으로 비빕니다."),
                        List.of("데치기", "볶기", "양념", "담기")
                ),
                new MenuCandidateResponse(
                        "D",
                        "저탄소 채소 덮밥",
                        "LOW_CARBON",
                        mainUses,
                        List.of("식용유", "간장", "후추"),
                        List.of(),
                        35,
                        "LOW",
                        "공용 키트만으로 조리 가능하고 추가구매 부담이 낮은 후보입니다.",
                        List.of("재료를 한입 크기로 썹니다.", "센 불에 빠르게 볶습니다.", "밥 위에 얹어 마무리합니다."),
                        List.of("손질", "볶기", "밥 준비", "마무리")
                )
        );
    }

    private List<PurchaseItemResponse> purchaseItems(String purchaserNickname) {
        if (purchaserNickname == null) {
            return List.of();
        }

        return List.of(new PurchaseItemResponse(
                "계란",
                "EGG",
                new BigDecimal("240"),
                List.of("egg"),
                purchaserNickname,
                3000
        ));
    }

    private List<CandidateUsedIngredientResponse> toCandidateUses(List<IngredientPoolItem> poolItems) {
        return poolItems.stream()
                .limit(5)
                .map(item -> {
                    BigDecimal plannedUseGrams = item.estimatedTotalGrams()
                            .multiply(DEFAULT_USE_RATIO)
                            .setScale(1, RoundingMode.HALF_UP);
                    return new CandidateUsedIngredientResponse(
                            item.ingredientId(),
                            item.nameKo(),
                            item.estimatedTotalGrams(),
                            plannedUseGrams,
                            DEFAULT_USE_RATIO
                    );
                })
                .toList();
    }

    private String writeCandidatesJson(List<MenuCandidateResponse> candidates) {
        try {
            return objectMapper.writeValueAsString(candidates);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Candidates cannot be serialized.", e);
        }
    }

    private List<MenuCandidateResponse> readCandidates(String candidatesJson) {
        if (!StringUtils.hasText(candidatesJson)) {
            return List.of();
        }

        try {
            return objectMapper.readValue(unwrapStoredJson(candidatesJson), new TypeReference<>() {
            });
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Menu candidates cannot be parsed.", e);
        }
    }

    private MenuCandidateResponse readSelectedMenu(String selectedMenuJson) {
        if (!StringUtils.hasText(selectedMenuJson)) {
            return null;
        }

        try {
            return objectMapper.readValue(unwrapStoredJson(selectedMenuJson), MenuCandidateResponse.class);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Selected menu cannot be parsed.", e);
        }
    }

    private String unwrapStoredJson(String value) throws JsonProcessingException {
        String unwrapped = value.trim();
        for (int i = 0; i < 3; i++) {
            if (!unwrapped.startsWith("\"") || !unwrapped.endsWith("\"")) {
                return unwrapped;
            }
            unwrapped = objectMapper.readValue(unwrapped, String.class).trim();
        }
        return unwrapped;
    }

    private static class IngredientPoolItem {

        private final Long ingredientId;
        private final String nameKo;
        private BigDecimal estimatedTotalGrams = BigDecimal.ZERO;

        private IngredientPoolItem(Long ingredientId, String nameKo) {
            this.ingredientId = ingredientId;
            this.nameKo = nameKo;
        }

        private Long ingredientId() {
            return ingredientId;
        }

        private String nameKo() {
            return nameKo;
        }

        private BigDecimal estimatedTotalGrams() {
            return estimatedTotalGrams;
        }

        private void add(BigDecimal grams) {
            estimatedTotalGrams = estimatedTotalGrams.add(grams);
        }
    }
}
