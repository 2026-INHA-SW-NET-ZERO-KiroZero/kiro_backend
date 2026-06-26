package com.kirozero.netzero.domain.result.repository;

import com.kirozero.netzero.domain.result.entity.ConsumptionRecordItem;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConsumptionRecordItemRepository extends JpaRepository<ConsumptionRecordItem, Long> {

    @EntityGraph(attributePaths = {"sessionIngredient", "sessionIngredient.ingredient"})
    List<ConsumptionRecordItem> findByRecordIdOrderByIdAsc(Long recordId);

    @EntityGraph(attributePaths = {"record", "sessionIngredient", "sessionIngredient.ingredient"})
    List<ConsumptionRecordItem> findByRecordIdIn(Collection<Long> recordIds);
}
