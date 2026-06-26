package com.kirozero.netzero.domain.recommendation.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kirozero.netzero.domain.ai.exception.AiGenerationException;
import com.kirozero.netzero.domain.ai.model.AiIngredient;
import com.kirozero.netzero.domain.ai.model.AiParticipant;
import com.kirozero.netzero.domain.ai.model.MenuCandidateGenerationContext;
import com.kirozero.netzero.domain.ai.model.RawMenuCandidate;
import com.kirozero.netzero.domain.ai.port.AiGenerationPort;
import com.kirozero.netzero.domain.auth.service.AuthService;
import com.kirozero.netzero.domain.ingredient.entity.IngredientMaster;
import com.kirozero.netzero.domain.ingredient.repository.IngredientMasterRepository;
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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class RecommendationService {

    private static final Logger log = LoggerFactory.getLogger(RecommendationService.class);
    private static final List<String> COMMON_KIT = List.of("식용유", "간장", "소금", "후추", "참기름");
    private static final int FINAL_CANDIDATE_COUNT = 4;
    private static final int FINAL_PER_TYPE = 2;
    private static final List<String> CANDIDATE_LABELS = List.of("A", "B", "C", "D");

    private final AuthService authService;
    private final SlotRepository slotRepository;
    private final SessionParticipantRepository sessionParticipantRepository;
    private final SessionIngredientRepository sessionIngredientRepository;
    private final IngredientMasterRepository ingredientMasterRepository;
    private final AiGenerationPort aiGenerationPort;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Random random = new Random();

    @Transactional
    public RecommendationResponse recommend(Long slotId, String authorizationHeader, RecommendationRequest request) {
        User user = authService.requireUser(authorizationHeader);
        Slot slot = slotRepository.findById(slotId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Slot not found."));
        List<SessionParticipant> participants = sessionParticipantRepository.findBySlotIdOrderByJoinedAtAsc(slotId);
        List<SessionIngredient> ingredients = sessionIngredientRepository.findBySlotIdOrderByIdAsc(slotId);

        validateRecommendationRequest(slot, user, participants, ingredients);

        List<AiIngredient> sharedPool = buildPoolItems(ingredients);
        Map<Long, AiIngredient> sharedPoolByIngredientId = sharedPool.stream()
                .collect(java.util.stream.Collectors.toMap(
                        AiIngredient::ingredientId,
                        item -> item,
                        (left, right) -> left,
                        LinkedHashMap::new
                ));
        Set<Long> sharedPoolIngredientIds = sharedPool.stream()
                .map(AiIngredient::ingredientId)
                .collect(java.util.stream.Collectors.toCollection(HashSet::new));
        Set<String> unionAllergyTags = collectUnionAllergyTags(participants);
        Map<Long, Set<String>> ingredientAllergyMap = buildIngredientAllergyMap(sharedPoolIngredientIds);
        List<SessionParticipant> purchasers = participants.stream()
                .filter(SessionParticipant::isCanPurchase)
                .toList();

        MenuCandidateGenerationContext context = new MenuCandidateGenerationContext(
                slot.getId(),
                sharedPool,
                buildParticipants(participants),
                COMMON_KIT
        );
        List<RawMenuCandidate> rawCandidates = aiGenerationPort.generateMenuCandidates(context);
        List<RawMenuCandidate> normalizedCandidates = rawCandidates.stream()
                .map(candidate -> MenuCandidatePolicy.normalizeCandidate(candidate, sharedPoolByIngredientId))
                .toList();

        List<RawMenuCandidate> survived = filterCandidates(
                normalizedCandidates,
                sharedPoolIngredientIds,
                unionAllergyTags,
                ingredientAllergyMap,
                !purchasers.isEmpty()
        );

        List<MenuCandidateResponse> finalCandidates = selectFinalFour(survived, purchasers);
        slot.proposeMenuCandidates(writeCandidatesJson(finalCandidates));

        return new RecommendationResponse(
                slot.getId(),
                slot.getRecommendationCount(),
                slot.getStatus(),
                finalCandidates
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

    private Set<String> collectUnionAllergyTags(List<SessionParticipant> participants) {
        Set<String> union = new HashSet<>();
        for (SessionParticipant participant : participants) {
            participant.getUser().getAllergies().forEach(allergy -> {
                if (StringUtils.hasText(allergy.getAllergenTag())) {
                    union.add(allergy.getAllergenTag().trim().toLowerCase());
                }
            });
        }
        return union;
    }

    private Map<Long, Set<String>> buildIngredientAllergyMap(Set<Long> ingredientIds) {
        if (ingredientIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, Set<String>> map = new LinkedHashMap<>();
        List<IngredientMaster> ingredients = ingredientMasterRepository.findAllById(ingredientIds);
        for (IngredientMaster ingredient : ingredients) {
            map.put(ingredient.getId(), parseAllergenTags(ingredient.getAllergenTagsJson()));
        }
        return map;
    }

    private Set<String> parseAllergenTags(String allergenTagsJson) {
        if (!StringUtils.hasText(allergenTagsJson)) {
            return Set.of();
        }
        try {
            List<String> tags = objectMapper.readValue(allergenTagsJson, new TypeReference<>() {
            });
            Set<String> normalized = new HashSet<>();
            for (String tag : tags) {
                if (StringUtils.hasText(tag)) {
                    normalized.add(tag.trim().toLowerCase());
                }
            }
            return normalized;
        } catch (JsonProcessingException e) {
            return Set.of();
        }
    }

    private List<RawMenuCandidate> filterCandidates(
            List<RawMenuCandidate> rawCandidates,
            Set<Long> sharedPoolIngredientIds,
            Set<String> unionAllergyTags,
            Map<Long, Set<String>> ingredientAllergyMap,
            boolean hasPurchaser
    ) {
        log.debug("Filter ctx: unionAllergyTags={}, sharedIds={}, ingredientAllergyMap={}, hasPurchaser={}",
                unionAllergyTags, sharedPoolIngredientIds, ingredientAllergyMap, hasPurchaser);
        List<RawMenuCandidate> survived = new ArrayList<>();
        for (RawMenuCandidate candidate : rawCandidates) {
            if (!hasPurchaser && !candidate.purchaseItems().isEmpty()) {
                log.debug("REJECT [{}] {}: purchaseItems present but no purchaser", candidate.menuType(), candidate.menuName());
                continue;
            }
            if (!usesOnlyKnownIngredients(candidate, sharedPoolIngredientIds)) {
                log.debug("REJECT [{}] {}: uses unknown ingredientId, used={}",
                        candidate.menuType(), candidate.menuName(),
                        candidate.usedLeftoverIngredients().stream().map(u -> u.ingredientId() + "/" + u.nameKo()).toList());
                continue;
            }
            if (MenuCandidatePolicy.hasBlockedLowCarbonPurchase(candidate)) {
                log.debug("REJECT [{}] {}: low carbon candidate contains animal purchase, purchases={}",
                        candidate.menuType(), candidate.menuName(),
                        candidate.purchaseItems().stream().map(p -> p.name() + "/" + p.category()).toList());
                continue;
            }
            if (conflictsWithAllergy(candidate, unionAllergyTags, ingredientAllergyMap)) {
                log.debug("REJECT [{}] {}: allergy conflict, used={}, purchases={}",
                        candidate.menuType(), candidate.menuName(),
                        candidate.usedLeftoverIngredients().stream().map(u -> u.ingredientId() + "/" + u.nameKo()).toList(),
                        candidate.purchaseItems().stream().map(p -> p.name() + "/" + p.allergenTags()).toList());
                continue;
            }
            log.debug("PASS [{}] {}", candidate.menuType(), candidate.menuName());
            survived.add(candidate);
        }
        return survived;
    }

    private boolean usesOnlyKnownIngredients(RawMenuCandidate candidate, Set<Long> knownIds) {
        for (CandidateUsedIngredientResponse used : candidate.usedLeftoverIngredients()) {
            if (!knownIds.contains(used.ingredientId())) {
                return false;
            }
        }
        return true;
    }

    private boolean conflictsWithAllergy(
            RawMenuCandidate candidate,
            Set<String> unionAllergyTags,
            Map<Long, Set<String>> ingredientAllergyMap
    ) {
        if (unionAllergyTags.isEmpty()) {
            return false;
        }
        for (CandidateUsedIngredientResponse used : candidate.usedLeftoverIngredients()) {
            Set<String> tags = ingredientAllergyMap.getOrDefault(used.ingredientId(), Set.of());
            if (intersects(tags, unionAllergyTags)) {
                return true;
            }
        }
        for (PurchaseItemResponse purchase : candidate.purchaseItems()) {
            if (purchase.allergenTags() == null) {
                continue;
            }
            for (String tag : purchase.allergenTags()) {
                if (StringUtils.hasText(tag) && unionAllergyTags.contains(tag.trim().toLowerCase())) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean intersects(Set<String> a, Set<String> b) {
        for (String value : a) {
            if (b.contains(value)) {
                return true;
            }
        }
        return false;
    }

    private List<MenuCandidateResponse> selectFinalFour(
            List<RawMenuCandidate> survived,
            List<SessionParticipant> purchasers
    ) {
        List<RawMenuCandidate> general = survived.stream()
                .filter(c -> "GENERAL".equals(c.menuType()))
                .toList();
        List<RawMenuCandidate> lowCarbon = survived.stream()
                .filter(c -> "LOW_CARBON".equals(c.menuType()))
                .toList();

        if (general.size() < FINAL_PER_TYPE || lowCarbon.size() < FINAL_PER_TYPE) {
            throw new AiGenerationException(
                    "Not enough candidates left after allergy/ingredient filtering: general="
                            + general.size() + ", lowCarbon=" + lowCarbon.size()
            );
        }

        List<RawMenuCandidate> picked = new ArrayList<>(FINAL_CANDIDATE_COUNT);
        picked.add(general.get(0));
        picked.add(general.get(1));
        picked.add(lowCarbon.get(0));
        picked.add(lowCarbon.get(1));

        List<MenuCandidateResponse> finalCandidates = new ArrayList<>(FINAL_CANDIDATE_COUNT);
        for (int i = 0; i < FINAL_CANDIDATE_COUNT; i++) {
            RawMenuCandidate raw = picked.get(i);
            String label = CANDIDATE_LABELS.get(i);
            finalCandidates.add(new MenuCandidateResponse(
                    label,
                    raw.menuName(),
                    raw.menuType(),
                    raw.usedLeftoverIngredients(),
                    raw.commonKitItems(),
                    assignPurchasers(raw.purchaseItems(), purchasers),
                    raw.cookingTimeMinutes(),
                    raw.difficulty(),
                    raw.recommendationReason(),
                    raw.cookingOutlineSteps(),
                    raw.rolePlanSummary()
            ));
        }
        return finalCandidates;
    }

    private List<PurchaseItemResponse> assignPurchasers(
            List<PurchaseItemResponse> purchaseItems,
            List<SessionParticipant> purchasers
    ) {
        if (purchaseItems.isEmpty() || purchasers.isEmpty()) {
            return purchaseItems;
        }
        String picked = purchasers.get(random.nextInt(purchasers.size())).getUser().getNickname();
        return purchaseItems.stream()
                .map(item -> new PurchaseItemResponse(
                        item.name(),
                        item.category(),
                        item.quantityGrams(),
                        item.allergenTags(),
                        picked,
                        item.estimatedCost()
                ))
                .toList();
    }

    private List<AiIngredient> buildPoolItems(List<SessionIngredient> ingredients) {
        Map<Long, IngredientPoolItemAccumulator> poolByIngredientId = new LinkedHashMap<>();
        for (SessionIngredient ingredient : ingredients) {
            poolByIngredientId
                    .computeIfAbsent(
                            ingredient.getIngredient().getId(),
                            ignored -> new IngredientPoolItemAccumulator(
                                    ingredient.getIngredient().getId(),
                                    ingredient.getIngredient().getNameKo()
                            )
                    )
                    .add(ingredient.getEstimatedGrams());
        }

        return poolByIngredientId.values().stream()
                .sorted(Comparator.comparing(IngredientPoolItemAccumulator::estimatedTotalGrams).reversed())
                .map(item -> new AiIngredient(item.ingredientId(), item.nameKo(), item.estimatedTotalGrams()))
                .toList();
    }

    private List<AiParticipant> buildParticipants(List<SessionParticipant> participants) {
        return participants.stream()
                .map(participant -> new AiParticipant(
                        participant.getId(),
                        participant.getUser().getNickname(),
                        participant.getUser().getCookingSkill().name(),
                        participant.isCanPurchase(),
                        participant.getUser().getAllergies().stream()
                                .map(allergy -> allergy.getAllergenTag())
                                .toList()
                ))
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

    private static class IngredientPoolItemAccumulator {

        private final Long ingredientId;
        private final String nameKo;
        private java.math.BigDecimal estimatedTotalGrams = java.math.BigDecimal.ZERO;

        private IngredientPoolItemAccumulator(Long ingredientId, String nameKo) {
            this.ingredientId = ingredientId;
            this.nameKo = nameKo;
        }

        private Long ingredientId() {
            return ingredientId;
        }

        private String nameKo() {
            return nameKo;
        }

        private java.math.BigDecimal estimatedTotalGrams() {
            return estimatedTotalGrams;
        }

        private void add(java.math.BigDecimal grams) {
            estimatedTotalGrams = estimatedTotalGrams.add(grams);
        }
    }
}
