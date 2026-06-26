package com.kirozero.netzero.global.exception;

import com.kirozero.netzero.domain.ai.exception.AiGenerationException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(AiGenerationException.class)
    public ResponseEntity<Map<String, Object>> handleAiGenerationException(AiGenerationException exception) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", Instant.now().toString());
        body.put("status", HttpStatus.BAD_GATEWAY.value());
        body.put("error", "AI Generation Failed");
        body.put("message", exception.getMessage());
        body.put("cause", rootCauseMessage(exception));
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(body);
    }

    private String rootCauseMessage(Throwable throwable) {
        Throwable cursor = throwable;
        while (cursor.getCause() != null) {
            cursor = cursor.getCause();
        }
        String message = cursor.getMessage();
        return cursor.getClass().getSimpleName() + (message == null ? "" : ": " + message);
    }
}
