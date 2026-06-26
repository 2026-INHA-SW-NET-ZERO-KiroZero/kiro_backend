package com.kirozero.netzero.domain.dashboard.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@IdClass(DashboardDailyIngredientId.class)
@Table(
        name = "dashboard_daily_ingredient",
        indexes = @Index(name = "idx_date", columnList = "event_date")
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DashboardDailyIngredient {

    @Id
    @Column(name = "event_date")
    private LocalDate eventDate;

    @Id
    @Column(name = "ingredient_name", length = 50)
    private String ingredientName;

    @Column(name = "total_used_grams", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalUsedGrams;

    @Column(name = "total_leftover_grams", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalLeftoverGrams;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
