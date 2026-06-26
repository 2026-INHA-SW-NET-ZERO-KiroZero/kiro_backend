package com.kirozero.netzero.domain.dashboard.batch;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class EventLineParser {

    private static final Logger log = LoggerFactory.getLogger(EventLineParser.class);

    private final ObjectMapper objectMapper = new ObjectMapper();

    public ParsedEvent parse(String line) {
        if (line == null || line.isBlank()) {
            return null;
        }
        try {
            JsonNode node = objectMapper.readTree(line);
            String event = node.path("event").asText(null);
            return event == null ? null : new ParsedEvent(event, node);
        } catch (Exception ex) {
            log.warn("broken event log line skipped: {}", abbreviate(line), ex);
            return null;
        }
    }

    private String abbreviate(String line) {
        return line.length() <= 200 ? line : line.substring(0, 200);
    }
}
