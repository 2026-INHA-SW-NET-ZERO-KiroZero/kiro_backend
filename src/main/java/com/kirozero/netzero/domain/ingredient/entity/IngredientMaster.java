package com.kirozero.netzero.domain.ingredient.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "ingredient_master")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class IngredientMaster {

    @Id
    private Long id;

    @Column(name = "name_ko", nullable = false, unique = true, length = 80)
    private String nameKo;

    @Column(name = "grams_per_count", nullable = false, precision = 10, scale = 2)
    private BigDecimal gramsPerCount;

    @Column(name = "allergen_tags_json", nullable = false, columnDefinition = "json")
    private String allergenTagsJson;

    @Column(name = "carbon_factor_kgco2e_per_kg", nullable = false, precision = 10, scale = 4)
    private BigDecimal carbonFactorKgco2ePerKg;
}
