package com.kirozero.netzero.domain.result.repository;

import com.kirozero.netzero.domain.result.entity.ConsumptionRecord;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConsumptionRecordRepository extends JpaRepository<ConsumptionRecord, Long> {

    boolean existsBySlotId(Long slotId);

    @EntityGraph(attributePaths = {"slot", "submittedBy"})
    Optional<ConsumptionRecord> findBySlotId(Long slotId);

    @EntityGraph(attributePaths = {"slot"})
    List<ConsumptionRecord> findBySlotIdIn(Collection<Long> slotIds);
}
