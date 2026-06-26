package com.kirozero.netzero.domain.session.entity;

import com.kirozero.netzero.domain.ingredient.entity.IngredientMaster;
import com.kirozero.netzero.domain.slot.entity.Slot;
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
import java.math.BigDecimal;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "session_ingredients")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SessionIngredient extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "slot_id", nullable = false)
    private Slot slot;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "participant_id", nullable = false)
    private SessionParticipant participant;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ingredient_id", nullable = false)
    private IngredientMaster ingredient;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal count;

    @Column(name = "known_grams", precision = 10, scale = 2)
    private BigDecimal knownGrams;

    @Column(name = "estimated_grams", nullable = false, precision = 10, scale = 2)
    private BigDecimal estimatedGrams;

    private SessionIngredient(
            Slot slot,
            SessionParticipant participant,
            IngredientMaster ingredient,
            BigDecimal count,
            BigDecimal knownGrams,
            BigDecimal estimatedGrams
    ) {
        this.slot = slot;
        this.participant = participant;
        this.ingredient = ingredient;
        this.count = count;
        this.knownGrams = knownGrams;
        this.estimatedGrams = estimatedGrams;
    }

    public static SessionIngredient create(
            Slot slot,
            SessionParticipant participant,
            IngredientMaster ingredient,
            BigDecimal count,
            BigDecimal knownGrams,
            BigDecimal estimatedGrams
    ) {
        return new SessionIngredient(slot, participant, ingredient, count, knownGrams, estimatedGrams);
    }
}
