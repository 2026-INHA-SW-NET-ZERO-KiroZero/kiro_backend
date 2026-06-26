package com.kirozero.netzero.domain.dashboard.repository;

import com.kirozero.netzero.domain.dashboard.entity.BatchJobHistory;
import com.kirozero.netzero.domain.dashboard.entity.BatchJobStatus;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BatchJobHistoryRepository extends JpaRepository<BatchJobHistory, LocalDate> {

    @Query("select max(h.eventDate) from BatchJobHistory h where h.status = :status")
    Optional<LocalDate> findMaxEventDateByStatus(@Param("status") BatchJobStatus status);

    default Optional<LocalDate> findLastSuccessDate() {
        return findMaxEventDateByStatus(BatchJobStatus.SUCCESS);
    }
}
