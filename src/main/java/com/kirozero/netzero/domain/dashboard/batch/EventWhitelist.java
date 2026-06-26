package com.kirozero.netzero.domain.dashboard.batch;

import java.util.List;
import java.util.Map;

public final class EventWhitelist {

    private static final Map<String, List<String>> REQUIRED = Map.of(
            "session_completed", List.of(
                    "slot_id",
                    "date",
                    "total_leftover_used_grams",
                    "estimated_carbon_saved_kgco2e",
                    "participant_count",
                    "place_name"
            ),
            "ingredient_used", List.of(
                    "slot_id",
                    "date",
                    "ingredient_name",
                    "used_grams",
                    "leftover_grams"
            ),
            "participant_joined", List.of(
                    "slot_id",
                    "date",
                    "user_id"
            )
    );

    private EventWhitelist() {
    }

    public static boolean isAllowed(String event) {
        return REQUIRED.containsKey(event);
    }

    public static boolean hasRequiredFields(ParsedEvent event) {
        return REQUIRED.getOrDefault(event.event(), List.of()).stream()
                .allMatch(event::has);
    }
}
