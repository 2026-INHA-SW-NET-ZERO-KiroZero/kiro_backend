package com.kirozero.netzero.domain.result.entity;

import com.kirozero.netzero.domain.session.entity.SessionIngredient;
import com.kirozero.netzero.global.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
        name = "consumption_record_items",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_consumption_record_items_record_ingredient",
                columnNames = {"record_id", "session_ingredient_id"}
        )
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ConsumptionRecordItem extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "record_id", nullable = false)
    private ConsumptionRecord record;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_ingredient_id", nullable = false)
    private SessionIngredient sessionIngredient;

    @Column(name = "use_rate", nullable = false)
    private int useRate;

    @Column(name = "used_grams", nullable = false, precision = 12, scale = 2)
    private BigDecimal usedGrams;

    @Column(name = "estimated_carbon_saved_kgco2e", nullable = false, precision = 12, scale = 4)
    private BigDecimal estimatedCarbonSavedKgco2e;

    private ConsumptionRecordItem(
            ConsumptionRecord record,
            SessionIngredient sessionIngredient,
            int useRate,
            BigDecimal usedGrams,
            BigDecimal estimatedCarbonSavedKgco2e
    ) {
        this.record = record;
        this.sessionIngredient = sessionIngredient;
        this.useRate = useRate;
        this.usedGrams = usedGrams;
        this.estimatedCarbonSavedKgco2e = estimatedCarbonSavedKgco2e;
    }

    public static ConsumptionRecordItem create(
            ConsumptionRecord record,
            SessionIngredient sessionIngredient,
            int useRate,
            BigDecimal usedGrams,
            BigDecimal estimatedCarbonSavedKgco2e
    ) {
        return new ConsumptionRecordItem(record, sessionIngredient, useRate, usedGrams, estimatedCarbonSavedKgco2e);
    }
}
