package com.kirozero.netzero.domain.dashboard.controller;

import com.kirozero.netzero.domain.dashboard.batch.LogIngestScheduler;
import com.kirozero.netzero.domain.dashboard.dto.DailyTrendResponse;
import com.kirozero.netzero.domain.dashboard.dto.ImpactSummaryResponse;
import com.kirozero.netzero.domain.dashboard.dto.TopIngredientResponse;
import com.kirozero.netzero.domain.dashboard.dto.TopItemResponse;
import com.kirozero.netzero.domain.dashboard.service.DashboardQueryService;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/api")
@RequiredArgsConstructor
public class DashboardApiController {

    private final DashboardQueryService queryService;
    private final LogIngestScheduler scheduler;

    @GetMapping("/metrics/impact")
    public ImpactSummaryResponse impact() {
        return queryService.impact();
    }

    @GetMapping("/metrics/daily-trend")
    public DailyTrendResponse dailyTrend() {
        return queryService.dailyTrend();
    }

    @GetMapping("/metrics/top-places")
    public List<TopItemResponse> topPlaces() {
        return queryService.topPlaces();
    }

    @GetMapping("/metrics/top-used-ingredients")
    public List<TopIngredientResponse> topUsedIngredients() {
        return queryService.topUsedIngredients();
    }

    @GetMapping("/metrics/top-leftover-ingredients")
    public List<TopIngredientResponse> topLeftoverIngredients() {
        return queryService.topLeftoverIngredients();
    }

    @PostMapping("/batch/run")
    public Map<String, Object> runBatch(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        scheduler.runFor(date);
        return Map.of("triggered", date.toString());
    }
}
