package com.kirozero.netzero.domain.dashboard.service;

import com.kirozero.netzero.domain.dashboard.dto.DailyTrendResponse;
import com.kirozero.netzero.domain.dashboard.dto.ImpactSummaryResponse;
import com.kirozero.netzero.domain.dashboard.dto.TopItemResponse;
import com.kirozero.netzero.domain.dashboard.entity.DashboardDailyMetric;
import com.kirozero.netzero.domain.dashboard.repository.BatchJobHistoryRepository;
import com.kirozero.netzero.domain.dashboard.repository.DashboardDailyIngredientRepository;
import com.kirozero.netzero.domain.dashboard.repository.DashboardDailyMetricRepository;
import com.kirozero.netzero.domain.dashboard.repository.DashboardDailyPlaceRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardQueryService {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final BigDecimal GRAMS_PER_KG = BigDecimal.valueOf(1000);
    private static final BigDecimal KGCO2E_PER_TREE = BigDecimal.valueOf(22);

    private final DashboardDailyMetricRepository metricRepository;
    private final DashboardDailyIngredientRepository ingredientRepository;
    private final DashboardDailyPlaceRepository placeRepository;
    private final BatchJobHistoryRepository batchJobHistoryRepository;

    public ImpactSummaryResponse impact() {
        LocalDate from = today().minusDays(29);
        List<DashboardDailyMetric> metrics = metricRepository.findByEventDateGreaterThanEqualOrderByEventDate(from);

        long sessions = metrics.stream()
                .mapToLong(DashboardDailyMetric::getCompletedSessionCount)
                .sum();
        long participants = metrics.stream()
                .mapToLong(DashboardDailyMetric::getParticipantCount)
                .sum();
        BigDecimal foodGrams = metrics.stream()
                .map(DashboardDailyMetric::getTotalFoodGrams)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal carbonKg = metrics.stream()
                .map(DashboardDailyMetric::getTotalCarbonKgco2e)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal foodKg = foodGrams.divide(GRAMS_PER_KG, 4, RoundingMode.HALF_UP);
        BigDecimal treeEquivalent = carbonKg.divide(KGCO2E_PER_TREE, 4, RoundingMode.HALF_UP);

        return new ImpactSummaryResponse(
                round(foodKg, 2),
                round(carbonKg, 2),
                participants,
                sessions,
                round(treeEquivalent, 2)
        );
    }

    public DailyTrendResponse dailyTrend() {
        LocalDate from = today().minusDays(4);
        Map<LocalDate, DashboardDailyMetric> metricsByDate = metricRepository
                .findByEventDateGreaterThanEqualOrderByEventDate(from)
                .stream()
                .collect(Collectors.toMap(DashboardDailyMetric::getEventDate, Function.identity()));

        List<String> labels = new ArrayList<>();
        List<Long> sessions = new ArrayList<>();
        List<Long> participants = new ArrayList<>();
        List<Double> foodGrams = new ArrayList<>();
        List<Double> carbonKg = new ArrayList<>();

        for (int i = 0; i < 5; i++) {
            LocalDate date = from.plusDays(i);
            DashboardDailyMetric metric = metricsByDate.get(date);
            labels.add(date.toString());
            sessions.add(metric == null ? 0L : (long) metric.getCompletedSessionCount());
            participants.add(metric == null ? 0L : (long) metric.getParticipantCount());
            foodGrams.add(metric == null ? 0.0 : round(metric.getTotalFoodGrams(), 2));
            carbonKg.add(metric == null ? 0.0 : round(metric.getTotalCarbonKgco2e(), 2));
        }

        return new DailyTrendResponse(
                labels,
                new DailyTrendResponse.Series(sessions, participants, foodGrams, carbonKg)
        );
    }

    public List<TopItemResponse> topPlaces() {
        return toTopItems(placeRepository.findTopPlaces(today().minusDays(29), PageRequest.of(0, 5)));
    }

    public List<TopItemResponse> topUsedIngredients() {
        return toTopItems(ingredientRepository.findTopUsedIngredients(today().minusDays(29), PageRequest.of(0, 5)));
    }

    public List<TopItemResponse> topLeftoverIngredients() {
        return toTopItems(ingredientRepository.findTopLeftoverIngredients(today().minusDays(29), PageRequest.of(0, 5)));
    }

    public LocalDate findLastSuccessDate() {
        return batchJobHistoryRepository.findLastSuccessDate().orElse(null);
    }

    private List<TopItemResponse> toTopItems(List<Object[]> rows) {
        return rows.stream()
                .map(row -> new TopItemResponse((String) row[0], round((Number) row[1], 2)))
                .toList();
    }

    private LocalDate today() {
        return LocalDate.now(KST);
    }

    private double round(Number value, int scale) {
        if (value == null) {
            return 0.0;
        }
        return round(new BigDecimal(value.toString()), scale);
    }

    private double round(BigDecimal value, int scale) {
        return value.setScale(scale, RoundingMode.HALF_UP).doubleValue();
    }
}
