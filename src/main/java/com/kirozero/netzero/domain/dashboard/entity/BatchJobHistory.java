package com.kirozero.netzero.domain.dashboard.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "batch_job_history")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BatchJobHistory {

    @Id
    @Column(name = "event_date")
    private LocalDate eventDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private BatchJobStatus status;

    @Column(name = "session_rows")
    private Integer sessionRows;

    @Column(name = "ingredient_rows")
    private Integer ingredientRows;

    @Column(name = "participant_rows")
    private Integer participantRows;

    @Column(name = "error_message", length = 500)
    private String errorMessage;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "finished_at")
    private LocalDateTime finishedAt;

    private BatchJobHistory(LocalDate eventDate, LocalDateTime now) {
        this.eventDate = eventDate;
        markRunning(now);
    }

    public static BatchJobHistory running(LocalDate eventDate, LocalDateTime now) {
        return new BatchJobHistory(eventDate, now);
    }

    public void markRunning(LocalDateTime now) {
        this.status = BatchJobStatus.RUNNING;
        this.sessionRows = null;
        this.ingredientRows = null;
        this.participantRows = null;
        this.errorMessage = null;
        this.startedAt = now;
        this.finishedAt = null;
    }

    public void markSuccess(int sessionRows, int ingredientRows, int participantRows, LocalDateTime now) {
        this.status = BatchJobStatus.SUCCESS;
        this.sessionRows = sessionRows;
        this.ingredientRows = ingredientRows;
        this.participantRows = participantRows;
        this.errorMessage = null;
        this.finishedAt = now;
    }

    public void markEmpty(LocalDateTime now) {
        this.status = BatchJobStatus.EMPTY;
        this.sessionRows = 0;
        this.ingredientRows = 0;
        this.participantRows = 0;
        this.errorMessage = null;
        this.finishedAt = now;
    }

    public void markFailed(String message, LocalDateTime now) {
        this.status = BatchJobStatus.FAILED;
        this.errorMessage = abbreviate(message);
        this.finishedAt = now;
    }

    private String abbreviate(String message) {
        if (message == null) {
            return null;
        }
        return message.length() <= 500 ? message : message.substring(0, 500);
    }
}
