package com.kirozero.netzero.domain.dashboard.repository;

import com.kirozero.netzero.domain.dashboard.entity.DashboardDailyMetric;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DashboardDailyMetricRepository extends JpaRepository<DashboardDailyMetric, LocalDate> {

    List<DashboardDailyMetric> findByEventDateGreaterThanEqualOrderByEventDate(LocalDate from);
}
