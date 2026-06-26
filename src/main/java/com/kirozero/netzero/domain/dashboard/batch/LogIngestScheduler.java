package com.kirozero.netzero.domain.dashboard.batch;

import java.time.LocalDate;
import java.time.ZoneId;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class LogIngestScheduler {

    private static final Logger log = LoggerFactory.getLogger(LogIngestScheduler.class);
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final LogIngestJob job;
    private final BatchHistoryService history;

    @Scheduled(cron = "0 0 3 * * *", zone = "Asia/Seoul")
    public void runDaily() {
        runFor(LocalDate.now(KST).minusDays(1));
    }

    public void runFor(LocalDate targetDate) {
        try {
            job.run(targetDate);
        } catch (Exception ex) {
            history.markFailed(targetDate, ex.getMessage());
            log.warn("event log ingest failed: date={}", targetDate, ex);
        }
    }
}
