package com.kirozero.netzero.domain.dashboard.repository;

import com.kirozero.netzero.domain.dashboard.entity.DashboardDailyIngredient;
import com.kirozero.netzero.domain.dashboard.entity.DashboardDailyIngredientId;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DashboardDailyIngredientRepository
        extends JpaRepository<DashboardDailyIngredient, DashboardDailyIngredientId> {

    @Query("""
            select i.ingredientName, sum(i.totalUsedGrams)
            from DashboardDailyIngredient i
            where i.eventDate >= :from
            group by i.ingredientName
            order by sum(i.totalUsedGrams) desc
            """)
    List<Object[]> findTopUsedIngredients(@Param("from") LocalDate from, Pageable pageable);

    @Query("""
            select i.ingredientName, sum(i.totalLeftoverGrams)
            from DashboardDailyIngredient i
            where i.eventDate >= :from
            group by i.ingredientName
            order by sum(i.totalLeftoverGrams) desc
            """)
    List<Object[]> findTopLeftoverIngredients(@Param("from") LocalDate from, Pageable pageable);
}
