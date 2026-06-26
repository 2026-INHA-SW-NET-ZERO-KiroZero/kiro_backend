package com.kirozero.netzero.global.event;

import java.util.LinkedHashMap;
import java.util.Map;
import net.logstash.logback.argument.StructuredArguments;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 비즈니스 서비스에서 도메인 이벤트를 JSON Lines 로그로 발행하는 헬퍼.
 *
 * <p>{@code kiro.events} 로거(INFO)로 한 줄을 기록하며, Logback의 {@code EVENT_FILE}
 * appender가 {@code ${LOG_DIR}/events-YYYY-MM-DD.jsonl} 파일에 적재한다.
 * 발행 실패가 비즈니스 트랜잭션을 깨지 않도록 모든 예외를 내부에서 삼키고 WARN 한 줄만 남긴다.
 */
public final class EventLogger {

    private static final Logger LOG = LoggerFactory.getLogger("kiro.events");
    private static final Logger FALLBACK = LoggerFactory.getLogger(EventLogger.class);

    private EventLogger() {
    }

    /**
     * 이벤트 한 건을 발행한다.
     *
     * @param event  이벤트명 (예: {@code session_completed})
     * @param fields 이벤트 본문 필드. {@code event} 키가 마지막에 추가된다.
     */
    public static void emit(String event, Map<String, Object> fields) {
        try {
            Map<String, Object> merged = new LinkedHashMap<>(fields);
            merged.put("event", event);
            LOG.info("", StructuredArguments.entries(merged));
        } catch (Exception ex) {
            FALLBACK.warn("event emit failed: event={}", event, ex);
        }
    }
}
