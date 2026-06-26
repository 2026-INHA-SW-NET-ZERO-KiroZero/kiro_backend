package com.kirozero.netzero.domain.dashboard.entity;

import java.io.Serializable;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class DashboardDailyIngredientId implements Serializable {

    private LocalDate eventDate;
    private String ingredientName;
}
