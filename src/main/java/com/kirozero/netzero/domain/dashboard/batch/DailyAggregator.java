package com.kirozero.netzero.domain.dashboard.batch;

import java.time.LocalDate;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DailyAggregator {

    private static final String METRIC_UPSERT = """
            INSERT INTO dashboard_daily_metric
              (event_date, completed_session_count, participant_count,
               total_food_grams, total_carbon_kgco2e, avg_ingredient_use_rate,
               low_carbon_session_count, total_refund_amount, avg_refund_amount, updated_at)
            SELECT
              :d,
              (SELECT COUNT(*) FROM event_session_completed_raw WHERE event_date = :d),
              (SELECT COUNT(*) FROM event_participant_joined_raw WHERE event_date = :d),
              COALESCE((SELECT SUM(total_leftover_used_grams) FROM event_session_completed_raw WHERE event_date = :d), 0),
              COALESCE((SELECT SUM(estimated_carbon_saved_kgco2e) FROM event_session_completed_raw WHERE event_date = :d), 0),
              COALESCE((SELECT AVG(avg_ingredient_use_rate) FROM event_session_completed_raw WHERE event_date = :d), 0),
              COALESCE((SELECT SUM(CASE WHEN menu_type = 'LOW_CARBON' THEN 1 ELSE 0 END) FROM event_session_completed_raw WHERE event_date = :d), 0),
              COALESCE((SELECT SUM(total_refund_amount) FROM event_session_completed_raw WHERE event_date = :d), 0),
              COALESCE((SELECT AVG(total_refund_amount) FROM event_session_completed_raw WHERE event_date = :d), 0),
              NOW()
            ON DUPLICATE KEY UPDATE
              completed_session_count = VALUES(completed_session_count),
              participant_count       = VALUES(participant_count),
              total_food_grams        = VALUES(total_food_grams),
              total_carbon_kgco2e     = VALUES(total_carbon_kgco2e),
              avg_ingredient_use_rate = VALUES(avg_ingredient_use_rate),
              low_carbon_session_count = VALUES(low_carbon_session_count),
              total_refund_amount      = VALUES(total_refund_amount),
              avg_refund_amount        = VALUES(avg_refund_amount),
              updated_at              = VALUES(updated_at)
            """;

    private static final String INGREDIENT_INSERT = """
            INSERT INTO dashboard_daily_ingredient
              (event_date, ingredient_name, total_used_grams, total_leftover_grams, updated_at)
            SELECT event_date, ingredient_name, SUM(used_grams), SUM(leftover_grams), NOW()
            FROM event_ingredient_used_raw
            WHERE event_date = :d
            GROUP BY event_date, ingredient_name
            """;

    private static final String PLACE_INSERT = """
            INSERT INTO dashboard_daily_place
              (event_date, place_name, session_count, updated_at)
            SELECT event_date, place_name, COUNT(*), NOW()
            FROM event_session_completed_raw
            WHERE event_date = :d
            GROUP BY event_date, place_name
            """;

    private final NamedParameterJdbcTemplate jdbc;

    public void refreshDailyMetrics(LocalDate date) {
        jdbc.update(METRIC_UPSERT, Map.of("d", date));
    }

    public void refreshDailyIngredients(LocalDate date) {
        jdbc.update("DELETE FROM dashboard_daily_ingredient WHERE event_date = :d", Map.of("d", date));
        jdbc.update(INGREDIENT_INSERT, Map.of("d", date));
    }

    public void refreshDailyPlaces(LocalDate date) {
        jdbc.update("DELETE FROM dashboard_daily_place WHERE event_date = :d", Map.of("d", date));
        jdbc.update(PLACE_INSERT, Map.of("d", date));
    }
}
