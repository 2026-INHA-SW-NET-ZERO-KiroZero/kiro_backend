package com.kirozero.netzero.domain.result.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kirozero.netzero.domain.auth.service.AuthService;
import com.kirozero.netzero.domain.recommendation.dto.MenuCandidateResponse;
import com.kirozero.netzero.domain.result.dto.ConsumptionRecordItemRequest;
import com.kirozero.netzero.domain.result.dto.CreateConsumptionRecordRequest;
import com.kirozero.netzero.domain.result.dto.CreateConsumptionRecordResponse;
import com.kirozero.netzero.domain.result.dto.MonthlyResultSummaryResponse;
import com.kirozero.netzero.domain.result.dto.MyResultTotalResponse;
import com.kirozero.netzero.domain.result.dto.PhotoUrlsResponse;
import com.kirozero.netzero.domain.result.dto.SessionResultResponse;
import com.kirozero.netzero.domain.result.entity.ConsumptionRecord;
import com.kirozero.netzero.domain.result.entity.ConsumptionRecordItem;
import com.kirozero.netzero.domain.result.repository.ConsumptionRecordItemRepository;
import com.kirozero.netzero.domain.result.repository.ConsumptionRecordRepository;
import com.kirozero.netzero.domain.session.entity.SessionIngredient;
import com.kirozero.netzero.domain.session.entity.SessionParticipant;
import com.kirozero.netzero.domain.session.repository.SessionIngredientRepository;
import com.kirozero.netzero.domain.session.repository.SessionParticipantRepository;
import com.kirozero.netzero.domain.slot.entity.Slot;
import com.kirozero.netzero.domain.slot.repository.SlotRepository;
import com.kirozero.netzero.domain.user.entity.User;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class ConsumptionResultService {

    private static final Set<Integer> ALLOWED_RATES = Set.of(0, 25, 50, 75, 100);
    private static final int BASE_RESERVATION_CREDIT = 2000;
    private static final BigDecimal MAX_REFUND_RATE = new BigDecimal("0.50");

    private final AuthService authService;
    private final SlotRepository slotRepository;
    private final SessionParticipantRepository sessionParticipantRepository;
    private final SessionIngredientRepository sessionIngredientRepository;
    private final ConsumptionRecordRepository consumptionRecordRepository;
    private final ConsumptionRecordItemRepository consumptionRecordItemRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Transactional
    public CreateConsumptionRecordResponse createRecord(
            Long slotId,
            String authorizationHeader,
            CreateConsumptionRecordRequest request
    ) {
        User user = authService.requireUser(authorizationHeader);
        Slot slot = slotRepository.findById(slotId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Slot not found."));
        if (!sessionParticipantRepository.existsBySlotIdAndUserId(slotId, user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only participants can submit result.");
        }
        if (!StringUtils.hasText(slot.getSelectedMenuJson())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Selected menu is required before result submission.");
        }
        if (consumptionRecordRepository.existsBySlotId(slotId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Consumption record already exists.");
        }

        validateRate(request.finishedFoodRate(), "finishedFoodRate");
        List<SessionIngredient> sessionIngredients = sessionIngredientRepository.findBySlotIdOrderByIdAsc(slotId);
        Map<Long, SessionIngredient> ingredientById = new LinkedHashMap<>();
        for (SessionIngredient ingredient : sessionIngredients) {
            ingredientById.put(ingredient.getId(), ingredient);
        }
        validateItemCoverage(request.items(), ingredientById);

        MenuCandidateResponse selectedMenu = readSelectedMenu(slot.getSelectedMenuJson());
        Calculation calculation = calculate(request, ingredientById, selectedMenu);
        ConsumptionRecord record = consumptionRecordRepository.save(ConsumptionRecord.create(
                slot,
                user,
                request.finishedFoodRate(),
                request.cookedPhotoUrl().trim(),
                request.afterPhotoUrl().trim(),
                calculation.totalUsedGrams(),
                calculation.avgIngredientUseRate(),
                calculation.estimatedCarbonSavedKgco2e(),
                calculation.lowCarbonSelected(),
                calculation.refundScore(),
                calculation.refundAmountPerUser()
        ));

        consumptionRecordItemRepository.saveAll(request.items().stream()
                .map(item -> createItem(record, ingredientById.get(item.sessionIngredientId()), item.useRate()))
                .toList());
        slot.complete();

        return new CreateConsumptionRecordResponse(
                record.getId(),
                slot.getId(),
                record.getRefundScore(),
                record.getRefundAmountPerUser(),
                record.getTotalUsedGrams(),
                record.getEstimatedCarbonSavedKgco2e(),
                false,
                slot.getStatus()
        );
    }

    @Transactional(readOnly = true)
    public SessionResultResponse getSessionResult(Long slotId, String authorizationHeader) {
        User user = authService.requireUser(authorizationHeader);
        Slot slot = slotRepository.findById(slotId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Slot not found."));
        if (!sessionParticipantRepository.existsBySlotIdAndUserId(slotId, user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only participants can view result.");
        }
        ConsumptionRecord record = consumptionRecordRepository.findBySlotId(slotId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Session result not found."));
        MenuCandidateResponse selectedMenu = readSelectedMenu(slot.getSelectedMenuJson());
        int participantCount = (int) sessionParticipantRepository.countBySlotId(slotId);

        return toSessionResult(record, selectedMenu, participantCount);
    }

    @Transactional(readOnly = true)
    public MyResultTotalResponse getMyResultTotal(String authorizationHeader) {
        User user = authService.requireUser(authorizationHeader);
        List<Long> slotIds = sessionParticipantRepository.findByUserIdOrderByJoinedAtDesc(user.getId()).stream()
                .map(participant -> participant.getSlot().getId())
                .toList();
        if (slotIds.isEmpty()) {
            return new MyResultTotalResponse(0, BigDecimal.ZERO, BigDecimal.ZERO, 0, List.of());
        }

        List<ConsumptionRecord> records = consumptionRecordRepository.findBySlotIdIn(slotIds);
        BigDecimal totalUsedGrams = records.stream()
                .map(ConsumptionRecord::getTotalUsedGrams)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalCarbon = records.stream()
                .map(ConsumptionRecord::getEstimatedCarbonSavedKgco2e)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        int totalRefund = records.stream()
                .mapToInt(ConsumptionRecord::getRefundAmountPerUser)
                .sum();

        return new MyResultTotalResponse(
                records.size(),
                totalUsedGrams,
                totalCarbon,
                totalRefund,
                monthlyResults(records)
        );
    }

    private List<MonthlyResultSummaryResponse> monthlyResults(List<ConsumptionRecord> records) {
        return records.stream()
                .collect(Collectors.groupingBy(record -> YearMonth.from(record.getCreatedAt())))
                .entrySet()
                .stream()
                .sorted(Map.Entry.<YearMonth, List<ConsumptionRecord>>comparingByKey(Comparator.reverseOrder()))
                .map(entry -> monthlyResult(entry.getKey(), entry.getValue()))
                .toList();
    }

    private MonthlyResultSummaryResponse monthlyResult(YearMonth yearMonth, List<ConsumptionRecord> records) {
        BigDecimal totalUsedGrams = records.stream()
                .map(ConsumptionRecord::getTotalUsedGrams)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalCarbon = records.stream()
                .map(ConsumptionRecord::getEstimatedCarbonSavedKgco2e)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        int totalRefund = records.stream()
                .mapToInt(ConsumptionRecord::getRefundAmountPerUser)
                .sum();

        return new MonthlyResultSummaryResponse(
                yearMonth.toString(),
                records.size(),
                totalUsedGrams,
                totalCarbon,
                totalRefund
        );
    }

    private void validateItemCoverage(
            List<ConsumptionRecordItemRequest> items,
            Map<Long, SessionIngredient> ingredientById
    ) {
        Map<Long, Integer> requestedRateByIngredientId = new LinkedHashMap<>();
        for (ConsumptionRecordItemRequest item : items) {
            if (requestedRateByIngredientId.put(item.sessionIngredientId(), item.useRate()) != null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Duplicated sessionIngredientId.");
            }
            validateRate(item.useRate(), "useRate");
        }
        if (!requestedRateByIngredientId.keySet().equals(ingredientById.keySet())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "All session ingredients must be submitted.");
        }
    }

    private void validateRate(Integer rate, String fieldName) {
        if (rate == null || !ALLOWED_RATES.contains(rate)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, fieldName + " must be one of 0, 25, 50, 75, 100.");
        }
    }

    private Calculation calculate(
            CreateConsumptionRecordRequest request,
            Map<Long, SessionIngredient> ingredientById,
            MenuCandidateResponse selectedMenu
    ) {
        BigDecimal totalUsedGrams = BigDecimal.ZERO;
        BigDecimal totalCarbon = BigDecimal.ZERO;
        int totalUseRate = 0;
        for (ConsumptionRecordItemRequest item : request.items()) {
            SessionIngredient ingredient = ingredientById.get(item.sessionIngredientId());
            totalUseRate += item.useRate();
            BigDecimal usedGrams = usedGrams(ingredient, item.useRate());
            totalUsedGrams = totalUsedGrams.add(usedGrams);
            totalCarbon = totalCarbon.add(carbonSaved(ingredient, usedGrams));
        }

        int avgUseRate = Math.round((float) totalUseRate / request.items().size());
        boolean lowCarbonSelected = "LOW_CARBON".equals(selectedMenu.menuType());
        int refundScore = refundScore(lowCarbonSelected, avgUseRate, request.finishedFoodRate());
        int refundAmountPerUser = refundAmountPerUser(refundScore);

        return new Calculation(
                totalUsedGrams.setScale(2, RoundingMode.HALF_UP),
                avgUseRate,
                totalCarbon.setScale(4, RoundingMode.HALF_UP),
                lowCarbonSelected,
                refundScore,
                refundAmountPerUser
        );
    }

    private ConsumptionRecordItem createItem(ConsumptionRecord record, SessionIngredient ingredient, int useRate) {
        BigDecimal usedGrams = usedGrams(ingredient, useRate);
        return ConsumptionRecordItem.create(
                record,
                ingredient,
                useRate,
                usedGrams,
                carbonSaved(ingredient, usedGrams)
        );
    }

    private BigDecimal usedGrams(SessionIngredient ingredient, int useRate) {
        return ingredient.getEstimatedGrams()
                .multiply(BigDecimal.valueOf(useRate))
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal carbonSaved(SessionIngredient ingredient, BigDecimal usedGrams) {
        return usedGrams
                .divide(BigDecimal.valueOf(1000), 6, RoundingMode.HALF_UP)
                .multiply(ingredient.getIngredient().getCarbonFactorKgco2ePerKg())
                .setScale(4, RoundingMode.HALF_UP);
    }

    private int refundScore(boolean lowCarbonSelected, int avgUseRate, int finishedFoodRate) {
        int lowCarbonScore = lowCarbonSelected ? 40 : 0;
        int ingredientScore = Math.round(avgUseRate * 0.30f);
        int finishedFoodScore = Math.round(finishedFoodRate * 0.30f);
        return Math.min(100, lowCarbonScore + ingredientScore + finishedFoodScore);
    }

    private int refundAmountPerUser(int refundScore) {
        return BigDecimal.valueOf(BASE_RESERVATION_CREDIT)
                .multiply(MAX_REFUND_RATE)
                .multiply(BigDecimal.valueOf(refundScore))
                .divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP)
                .intValue();
    }

    private SessionResultResponse toSessionResult(
            ConsumptionRecord record,
            MenuCandidateResponse selectedMenu,
            int participantCount
    ) {
        String summaryText = participantCount + "명이 함께 남은 재료 "
                + record.getTotalUsedGrams().stripTrailingZeros().toPlainString()
                + "g을 한 끼로 소진했습니다.";

        return new SessionResultResponse(
                record.getSlot().getId(),
                selectedMenu.menuName(),
                selectedMenu.menuType(),
                record.getTotalUsedGrams(),
                record.getAvgIngredientUseRate(),
                record.getFinishedFoodRate(),
                record.getTotalUsedGrams(),
                record.getEstimatedCarbonSavedKgco2e(),
                record.isLowCarbonSelected(),
                record.getRefundScore(),
                record.getRefundAmountPerUser(),
                record.getRefundAmountPerUser() * participantCount,
                summaryText,
                new PhotoUrlsResponse(record.getCookedPhotoUrl(), record.getAfterPhotoUrl())
        );
    }

    private MenuCandidateResponse readSelectedMenu(String selectedMenuJson) {
        if (!StringUtils.hasText(selectedMenuJson)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Selected menu is required.");
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

    private record Calculation(
            BigDecimal totalUsedGrams,
            int avgIngredientUseRate,
            BigDecimal estimatedCarbonSavedKgco2e,
            boolean lowCarbonSelected,
            int refundScore,
            int refundAmountPerUser
    ) {
    }
}
