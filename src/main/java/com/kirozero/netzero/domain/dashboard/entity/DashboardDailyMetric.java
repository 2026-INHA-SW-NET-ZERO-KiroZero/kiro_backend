package com.kirozero.netzero.domain.dashboard.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "dashboard_daily_metric")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DashboardDailyMetric {

    @Id
    @Column(name = "event_date")
    private LocalDate eventDate;

    @Column(name = "completed_session_count", nullable = false)
    private int completedSessionCount;

    @Column(name = "participant_count", nullable = false)
    private int participantCount;

    @Column(name = "total_food_grams", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalFoodGrams;

    @Column(name = "total_carbon_kgco2e", nullable = false, precision = 12, scale = 3)
    private BigDecimal totalCarbonKgco2e;

    @Column(name = "avg_ingredient_use_rate", precision = 5, scale = 2)
    private BigDecimal avgIngredientUseRate;

    @Column(name = "low_carbon_session_count")
    private Integer lowCarbonSessionCount;

    @Column(name = "total_refund_amount")
    private Integer totalRefundAmount;

    @Column(name = "avg_refund_amount", precision = 12, scale = 2)
    private BigDecimal avgRefundAmount;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
