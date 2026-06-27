package com.kirozero.netzero.domain.report.controller;

import com.kirozero.netzero.domain.report.batch.MonthlyReportScheduler;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/api/reports")
@RequiredArgsConstructor
public class MonthlyReportTestController {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final MonthlyReportScheduler scheduler;

    @PostMapping("/monthly/test")
    public Map<String, Object> triggerMonthlyReport(
            @RequestParam(required = false) YearMonth reportMonth
    ) {
        YearMonth targetMonth = reportMonth == null ? YearMonth.now(KST) : reportMonth;
        LocalDateTime triggeredAt = LocalDateTime.now(KST);

        boolean lambdaInvoked = scheduler.runFor(targetMonth, triggeredAt);

        return Map.of(
                "triggered", true,
                "lambdaInvoked", lambdaInvoked,
                "reportMonth", targetMonth.toString(),
                "triggeredAt", triggeredAt.toString()
        );
    }
}
