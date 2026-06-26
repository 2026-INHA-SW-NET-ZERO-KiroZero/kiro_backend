package com.kirozero.netzero.domain.result.entity;

import com.kirozero.netzero.domain.slot.entity.Slot;
import com.kirozero.netzero.domain.user.entity.User;
import com.kirozero.netzero.global.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
        name = "consumption_records",
        uniqueConstraints = @UniqueConstraint(name = "uk_consumption_records_slot", columnNames = "slot_id")
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ConsumptionRecord extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "slot_id", nullable = false)
    private Slot slot;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "submitted_by", nullable = false)
    private User submittedBy;

    @Column(name = "finished_food_rate", nullable = false)
    private int finishedFoodRate;

    @Column(name = "cooked_photo_url", nullable = false, length = 500)
    private String cookedPhotoUrl;

    @Column(name = "after_photo_url", nullable = false, length = 500)
    private String afterPhotoUrl;

    @Column(name = "total_used_grams", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalUsedGrams;

    @Column(name = "avg_ingredient_use_rate", nullable = false)
    private int avgIngredientUseRate;

    @Column(name = "estimated_carbon_saved_kgco2e", nullable = false, precision = 12, scale = 4)
    private BigDecimal estimatedCarbonSavedKgco2e;

    @Column(name = "low_carbon_selected", nullable = false)
    private boolean lowCarbonSelected;

    @Column(name = "refund_score", nullable = false)
    private int refundScore;

    @Column(name = "refund_amount_per_user", nullable = false)
    private int refundAmountPerUser;

    @OneToMany(mappedBy = "record")
    private List<ConsumptionRecordItem> items = new ArrayList<>();

    private ConsumptionRecord(
            Slot slot,
            User submittedBy,
            int finishedFoodRate,
            String cookedPhotoUrl,
            String afterPhotoUrl,
            BigDecimal totalUsedGrams,
            int avgIngredientUseRate,
            BigDecimal estimatedCarbonSavedKgco2e,
            boolean lowCarbonSelected,
            int refundScore,
            int refundAmountPerUser
    ) {
        this.slot = slot;
        this.submittedBy = submittedBy;
        this.finishedFoodRate = finishedFoodRate;
        this.cookedPhotoUrl = cookedPhotoUrl;
        this.afterPhotoUrl = afterPhotoUrl;
        this.totalUsedGrams = totalUsedGrams;
        this.avgIngredientUseRate = avgIngredientUseRate;
        this.estimatedCarbonSavedKgco2e = estimatedCarbonSavedKgco2e;
        this.lowCarbonSelected = lowCarbonSelected;
        this.refundScore = refundScore;
        this.refundAmountPerUser = refundAmountPerUser;
    }

    public static ConsumptionRecord create(
            Slot slot,
            User submittedBy,
            int finishedFoodRate,
            String cookedPhotoUrl,
            String afterPhotoUrl,
            BigDecimal totalUsedGrams,
            int avgIngredientUseRate,
            BigDecimal estimatedCarbonSavedKgco2e,
            boolean lowCarbonSelected,
            int refundScore,
            int refundAmountPerUser
    ) {
        return new ConsumptionRecord(
                slot,
                submittedBy,
                finishedFoodRate,
                cookedPhotoUrl,
                afterPhotoUrl,
                totalUsedGrams,
                avgIngredientUseRate,
                estimatedCarbonSavedKgco2e,
                lowCarbonSelected,
                refundScore,
                refundAmountPerUser
        );
    }
}
