package com.kirozero.netzero.domain.dashboard.batch;

import com.kirozero.netzero.domain.dashboard.entity.BatchJobHistory;
import com.kirozero.netzero.domain.dashboard.entity.BatchJobStatus;
import com.kirozero.netzero.domain.dashboard.repository.BatchJobHistoryRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BatchHistoryService {

    private final BatchJobHistoryRepository repository;

    @Transactional(readOnly = true)
    public boolean isAlreadySucceeded(LocalDate date) {
        return repository.findById(date)
                .map(BatchJobHistory::getStatus)
                .filter(BatchJobStatus.SUCCESS::equals)
                .isPresent();
    }

    @Transactional
    public void markRunning(LocalDate date) {
        LocalDateTime now = LocalDateTime.now();
        BatchJobHistory history = repository.findById(date)
                .orElseGet(() -> BatchJobHistory.running(date, now));
        history.markRunning(now);
        repository.save(history);
    }

    @Transactional
    public void markSuccess(LocalDate date, int sessionRows, int ingredientRows, int participantRows) {
        BatchJobHistory history = repository.findById(date)
                .orElseGet(() -> BatchJobHistory.running(date, LocalDateTime.now()));
        history.markSuccess(sessionRows, ingredientRows, participantRows, LocalDateTime.now());
        repository.save(history);
    }

    @Transactional
    public void markEmpty(LocalDate date) {
        BatchJobHistory history = repository.findById(date)
                .orElseGet(() -> BatchJobHistory.running(date, LocalDateTime.now()));
        history.markEmpty(LocalDateTime.now());
        repository.save(history);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markFailed(LocalDate date, String message) {
        BatchJobHistory history = repository.findById(date)
                .orElseGet(() -> BatchJobHistory.running(date, LocalDateTime.now()));
        history.markFailed(message, LocalDateTime.now());
        repository.save(history);
    }
}
