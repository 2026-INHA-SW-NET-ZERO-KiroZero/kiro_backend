package com.kirozero.netzero.domain.dashboard.batch;

import java.time.LocalDate;
import java.math.BigDecimal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class RawInsertRepository {

    private final JdbcTemplate jdbc;

    public void bulkInsertSessions(List<SessionCompletedRow> rows) {
        if (rows.isEmpty()) {
            return;
        }
        jdbc.batchUpdate("""
                INSERT INTO event_session_completed_raw
                  (event_date, slot_id, place_name, station_code, menu_name, menu_type,
                   participant_count, finished_food_rate, total_leftover_input_grams,
                   total_leftover_used_grams, avg_ingredient_use_rate,
                   estimated_food_waste_reduced_grams, estimated_carbon_saved_kgco2e,
                   refund_amount_per_user, total_refund_amount, created_at)
                VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,NOW())
                """, rows, 1000, (ps, row) -> {
            ps.setObject(1, row.eventDate());
            ps.setObject(2, row.slotId());
            ps.setObject(3, row.placeName());
            ps.setObject(4, row.stationCode());
            ps.setObject(5, row.menuName());
            ps.setObject(6, row.menuType());
            ps.setObject(7, row.participantCount());
            ps.setObject(8, row.finishedFoodRate());
            ps.setObject(9, row.totalLeftoverInputGrams());
            ps.setObject(10, row.totalLeftoverUsedGrams());
            ps.setObject(11, row.avgIngredientUseRate());
            ps.setObject(12, row.estimatedFoodWasteReducedGrams());
            ps.setObject(13, row.estimatedCarbonSavedKgco2e());
            ps.setObject(14, row.refundAmountPerUser());
            ps.setObject(15, row.totalRefundAmount());
        });
    }

    public void bulkInsertIngredients(List<IngredientUsedRow> rows) {
        if (rows.isEmpty()) {
            return;
        }
        jdbc.batchUpdate("""
                INSERT INTO event_ingredient_used_raw
                  (event_date, slot_id, ingredient_name, input_grams, used_grams,
                   leftover_grams, use_rate, created_at)
                VALUES (?,?,?,?,?,?,?,NOW())
                """, rows, 1000, (ps, row) -> {
            ps.setObject(1, row.eventDate());
            ps.setObject(2, row.slotId());
            ps.setObject(3, row.ingredientName());
            ps.setObject(4, row.inputGrams());
            ps.setObject(5, row.usedGrams());
            ps.setObject(6, row.leftoverGrams());
            ps.setObject(7, row.useRate());
        });
    }

    public void bulkInsertParticipants(List<ParticipantJoinedRow> rows) {
        if (rows.isEmpty()) {
            return;
        }
        jdbc.batchUpdate("""
                INSERT INTO event_participant_joined_raw
                  (event_date, slot_id, user_id, created_at)
                VALUES (?,?,?,NOW())
                """, rows, 1000, (ps, row) -> {
            ps.setObject(1, row.eventDate());
            ps.setObject(2, row.slotId());
            ps.setObject(3, row.userId());
        });
    }

    public record SessionCompletedRow(
            LocalDate eventDate,
            Long slotId,
            String placeName,
            String stationCode,
            String menuName,
            String menuType,
            Integer participantCount,
            Integer finishedFoodRate,
            BigDecimal totalLeftoverInputGrams,
            BigDecimal totalLeftoverUsedGrams,
            Integer avgIngredientUseRate,
            BigDecimal estimatedFoodWasteReducedGrams,
            BigDecimal estimatedCarbonSavedKgco2e,
            Integer refundAmountPerUser,
            Integer totalRefundAmount
    ) {

        static SessionCompletedRow of(ParsedEvent event) {
            return new SessionCompletedRow(
                    event.localDate("date"),
                    event.longVal("slot_id"),
                    event.field("place_name"),
                    event.field("station_code"),
                    event.field("menu_name"),
                    event.field("menu_type"),
                    event.intVal("participant_count"),
                    event.intVal("finished_food_rate"),
                    event.decimal("total_leftover_input_grams"),
                    event.decimal("total_leftover_used_grams"),
                    event.intVal("avg_ingredient_use_rate"),
                    event.decimal("estimated_food_waste_reduced_grams"),
                    event.decimal("estimated_carbon_saved_kgco2e"),
                    event.intVal("refund_amount_per_user"),
                    event.intVal("total_refund_amount")
            );
        }
    }

    public record IngredientUsedRow(
            LocalDate eventDate,
            Long slotId,
            String ingredientName,
            BigDecimal inputGrams,
            BigDecimal usedGrams,
            BigDecimal leftoverGrams,
            Integer useRate
    ) {

        static IngredientUsedRow of(ParsedEvent event) {
            return new IngredientUsedRow(
                    event.localDate("date"),
                    event.longVal("slot_id"),
                    event.field("ingredient_name"),
                    event.decimal("input_grams"),
                    event.decimal("used_grams"),
                    event.decimal("leftover_grams"),
                    event.intVal("use_rate")
            );
        }
    }

    public record ParticipantJoinedRow(
            LocalDate eventDate,
            Long slotId,
            Long userId
    ) {

        static ParticipantJoinedRow of(ParsedEvent event) {
            return new ParticipantJoinedRow(
                    event.localDate("date"),
                    event.longVal("slot_id"),
                    event.longVal("user_id")
            );
        }
    }
}
