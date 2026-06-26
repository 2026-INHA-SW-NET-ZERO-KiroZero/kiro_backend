package com.kirozero.netzero.domain.dashboard.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
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
@Table(
        name = "event_ingredient_used_raw",
        indexes = @Index(name = "idx_event_date", columnList = "event_date")
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EventIngredientUsedRaw {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_date", nullable = false)
    private LocalDate eventDate;

    @Column(name = "slot_id", nullable = false)
    private Long slotId;

    @Column(name = "ingredient_name", nullable = false, length = 50)
    private String ingredientName;

    @Column(name = "input_grams", precision = 10, scale = 2)
    private BigDecimal inputGrams;

    @Column(name = "used_grams", nullable = false, precision = 10, scale = 2)
    private BigDecimal usedGrams;

    @Column(name = "leftover_grams", nullable = false, precision = 10, scale = 2)
    private BigDecimal leftoverGrams;

    @Column(name = "use_rate")
    private Integer useRate;

    @Column(name = "created_at", nullable = false, columnDefinition = "datetime default current_timestamp")
    private LocalDateTime createdAt;
}
