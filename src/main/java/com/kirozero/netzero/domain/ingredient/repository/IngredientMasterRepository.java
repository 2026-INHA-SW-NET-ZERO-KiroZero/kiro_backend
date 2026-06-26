package com.kirozero.netzero.domain.ingredient.repository;

import com.kirozero.netzero.domain.ingredient.entity.IngredientMaster;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IngredientMasterRepository extends JpaRepository<IngredientMaster, Long> {

    List<IngredientMaster> findTop20ByOrderByIdAsc();

    List<IngredientMaster> findTop20ByNameKoContainingOrderByNameKoAsc(String keyword);
}
