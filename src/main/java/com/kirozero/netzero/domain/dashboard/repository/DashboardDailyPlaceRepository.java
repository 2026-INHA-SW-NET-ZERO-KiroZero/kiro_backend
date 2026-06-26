package com.kirozero.netzero.domain.dashboard.repository;

import com.kirozero.netzero.domain.dashboard.entity.DashboardDailyPlace;
import com.kirozero.netzero.domain.dashboard.entity.DashboardDailyPlaceId;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DashboardDailyPlaceRepository extends JpaRepository<DashboardDailyPlace, DashboardDailyPlaceId> {

    @Query("""
            select p.placeName, sum(p.sessionCount)
            from DashboardDailyPlace p
            where p.eventDate >= :from
            group by p.placeName
            order by sum(p.sessionCount) desc
            """)
    List<Object[]> findTopPlaces(@Param("from") LocalDate from, Pageable pageable);
}
