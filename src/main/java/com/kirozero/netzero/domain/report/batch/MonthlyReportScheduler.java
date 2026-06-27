package com.kirozero.netzero.domain.report.batch;

import com.kirozero.netzero.domain.report.config.MonthlyReportLambdaProperties;
import com.kirozero.netzero.domain.report.service.MonthlyReportLambdaInvoker;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MonthlyReportScheduler {

    private static final Logger log = LoggerFactory.getLogger(MonthlyReportScheduler.class);

    private final MonthlyReportLambdaProperties properties;
    private final MonthlyReportLambdaInvoker lambdaInvoker;

    @Scheduled(
            cron = "${netzero3.report.lambda.cron:0 0 9 1 * *}",
            zone = "${netzero3.report.lambda.zone:Asia/Seoul}"
    )
    public void runMonthly() {
        ZoneId zoneId = ZoneId.of(properties.getZone());
        YearMonth reportMonth = YearMonth.from(LocalDate.now(zoneId).minusMonths(1));
        LocalDateTime triggeredAt = LocalDateTime.now(zoneId);
        runFor(reportMonth, triggeredAt);
    }

    public boolean runFor(YearMonth reportMonth, LocalDateTime triggeredAt) {
        try {
            return lambdaInvoker.invoke(reportMonth, triggeredAt);
        } catch (Exception ex) {
            log.warn("monthly report lambda invocation failed: reportMonth={}", reportMonth, ex);
            return false;
        }
    }
}
