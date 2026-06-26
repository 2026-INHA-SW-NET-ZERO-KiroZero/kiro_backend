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
        name = "event_session_completed_raw",
        indexes = @Index(name = "idx_event_date", columnList = "event_date")
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EventSessionCompletedRaw {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_date", nullable = false)
    private LocalDate eventDate;

    @Column(name = "slot_id", nullable = false)
    private Long slotId;

    @Column(name = "place_name", nullable = false, length = 100)
    private String placeName;

    @Column(name = "station_code", length = 50)
    private String stationCode;

    @Column(name = "menu_name", length = 100)
    private String menuName;

    @Column(name = "menu_type", length = 20)
    private String menuType;

    @Column(name = "participant_count", nullable = false)
    private int participantCount;

    @Column(name = "finished_food_rate")
    private Integer finishedFoodRate;

    @Column(name = "total_leftover_input_grams", precision = 10, scale = 2)
    private BigDecimal totalLeftoverInputGrams;

    @Column(name = "total_leftover_used_grams", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalLeftoverUsedGrams;

    @Column(name = "avg_ingredient_use_rate")
    private Integer avgIngredientUseRate;

    @Column(name = "estimated_food_waste_reduced_grams", precision = 10, scale = 2)
    private BigDecimal estimatedFoodWasteReducedGrams;

    @Column(name = "estimated_carbon_saved_kgco2e", nullable = false, precision = 10, scale = 3)
    private BigDecimal estimatedCarbonSavedKgco2e;

    @Column(name = "created_at", nullable = false, columnDefinition = "datetime default current_timestamp")
    private LocalDateTime createdAt;
}
