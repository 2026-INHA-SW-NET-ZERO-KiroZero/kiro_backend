package com.kirozero.netzero.domain.slot.repository;

import com.kirozero.netzero.domain.slot.entity.Slot;
import com.kirozero.netzero.domain.slot.enums.SlotStatus;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SlotRepository extends JpaRepository<Slot, Long> {

    List<Slot> findByDateAndStatusOrderByStartTimeAscStationCodeAsc(LocalDate date, SlotStatus status);

    List<Slot> findByDateOrderByStartTimeAscStationCodeAsc(LocalDate date);
}
