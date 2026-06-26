package com.kirozero.netzero.domain.dashboard.batch;

import com.fasterxml.jackson.databind.JsonNode;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

public record ParsedEvent(String event, JsonNode node) {

    public boolean has(String fieldName) {
        JsonNode value = node.path(fieldName);
        return !value.isMissingNode() && !value.isNull();
    }

    public String field(String fieldName) {
        JsonNode value = node.path(fieldName);
        return value.isMissingNode() || value.isNull() ? null : value.asText();
    }

    public BigDecimal decimal(String fieldName) {
        String value = field(fieldName);
        return value == null ? null : new BigDecimal(value);
    }

    public Integer intVal(String fieldName) {
        JsonNode value = node.path(fieldName);
        return value.isMissingNode() || value.isNull() ? null : value.asInt();
    }

    public Long longVal(String fieldName) {
        JsonNode value = node.path(fieldName);
        return value.isMissingNode() || value.isNull() ? null : value.asLong();
    }

    public LocalDate localDate(String fieldName) {
        String value = field(fieldName);
        if (value == null) {
            return null;
        }
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException ex) {
            return null;
        }
    }
}
