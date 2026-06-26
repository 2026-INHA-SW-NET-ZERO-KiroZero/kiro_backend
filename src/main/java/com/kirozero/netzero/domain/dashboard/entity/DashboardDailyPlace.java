package com.kirozero.netzero.domain.dashboard.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@IdClass(DashboardDailyPlaceId.class)
@Table(
        name = "dashboard_daily_place",
        indexes = @Index(name = "idx_date", columnList = "event_date")
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DashboardDailyPlace {

    @Id
    @Column(name = "event_date")
    private LocalDate eventDate;

    @Id
    @Column(name = "place_name", length = 100)
    private String placeName;

    @Column(name = "session_count", nullable = false)
    private int sessionCount;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
