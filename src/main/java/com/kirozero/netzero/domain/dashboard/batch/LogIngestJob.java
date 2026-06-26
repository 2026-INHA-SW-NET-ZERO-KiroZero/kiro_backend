package com.kirozero.netzero.domain.dashboard.batch;

import com.kirozero.netzero.domain.dashboard.batch.RawInsertRepository.IngredientUsedRow;
import com.kirozero.netzero.domain.dashboard.batch.RawInsertRepository.ParticipantJoinedRow;
import com.kirozero.netzero.domain.dashboard.batch.RawInsertRepository.SessionCompletedRow;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class LogIngestJob {

    private final EventLineParser parser;
    private final RawInsertRepository rawRepository;
    private final DailyAggregator aggregator;
    private final BatchHistoryService history;

    @Value("${app.event-log-dir:/var/log/kiro}")
    private String logDir;

    @Transactional
    public void run(LocalDate targetDate) throws IOException {
        if (history.isAlreadySucceeded(targetDate)) {
            return;
        }
        history.markRunning(targetDate);

        Path file = Path.of(logDir, "events-" + targetDate + ".jsonl");
        if (!Files.exists(file)) {
            history.markEmpty(targetDate);
            return;
        }

        IngestCounts counts = ingestRaw(file, targetDate);
        aggregator.refreshDailyMetrics(targetDate);
        aggregator.refreshDailyIngredients(targetDate);
        aggregator.refreshDailyPlaces(targetDate);
        history.markSuccess(targetDate, counts.sessionRows(), counts.ingredientRows(), counts.participantRows());
    }

    private IngestCounts ingestRaw(Path file, LocalDate targetDate) throws IOException {
        List<SessionCompletedRow> sessions = new ArrayList<>();
        List<IngredientUsedRow> ingredients = new ArrayList<>();
        List<ParticipantJoinedRow> participants = new ArrayList<>();

        try (Stream<String> lines = Files.lines(file, StandardCharsets.UTF_8)) {
            lines.map(parser::parse)
                    .filter(event -> event != null && EventWhitelist.isAllowed(event.event()))
                    .filter(EventWhitelist::hasRequiredFields)
                    .filter(event -> targetDate.equals(event.localDate("date")))
                    .forEach(event -> append(event, sessions, ingredients, participants));
        }

        rawRepository.bulkInsertSessions(sessions);
        rawRepository.bulkInsertIngredients(ingredients);
        rawRepository.bulkInsertParticipants(participants);
        return new IngestCounts(sessions.size(), ingredients.size(), participants.size());
    }

    private void append(
            ParsedEvent event,
            List<SessionCompletedRow> sessions,
            List<IngredientUsedRow> ingredients,
            List<ParticipantJoinedRow> participants
    ) {
        switch (event.event()) {
            case "session_completed" -> sessions.add(SessionCompletedRow.of(event));
            case "ingredient_used" -> ingredients.add(IngredientUsedRow.of(event));
            case "participant_joined" -> participants.add(ParticipantJoinedRow.of(event));
            default -> {
            }
        }
    }

    private record IngestCounts(int sessionRows, int ingredientRows, int participantRows) {
    }
}
