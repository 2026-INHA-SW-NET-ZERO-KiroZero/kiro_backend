# 데이터 파이프라인 (파일 로그 → 일 배치 → RDS 집계 → 어드민) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Spring 비즈니스 서비스에서 발생한 도메인 이벤트를 일자별 JSON Lines 파일로 적재하고, 매일 새벽 03:00 KST `@Scheduled` 배치가 어제 파일을 한 번 읽어 화이트리스트 이벤트만 RDS(MySQL) raw 테이블에 INSERT 한 뒤 같은 트랜잭션에서 그 날짜 분만 GROUP BY 해 일별 집계 테이블을 UPSERT 한다. 어드민 대시보드는 일별 집계 테이블만 SELECT 해서 임팩트 카운터·일별 추이·식재료/장소 TOP을 렌더한다.

**Architecture:** Spring 서비스 메서드 안 `EventLogger.emit(...)` 한 줄 → Logback `RollingFileAppender`(LogstashEncoder) → `${LOG_DIR}/events-YYYY-MM-DD.jsonl` → `@Scheduled` 배치(`LogIngestJob`, 한 트랜잭션: 멱등 가드 → 파싱·필터 → raw BULK INSERT → 일별 집계 UPSERT) → RDS MySQL(raw 3 + 집계 3 + 히스토리 1) → `DashboardQueryService`(짧은 SELECT/SUM) → `DashboardApiController` JSON → Thymeleaf 페이지(Chart.js fetch). 도메인 이벤트/listener는 도입하지 않고 비즈니스 서비스 메서드 안에서 헬퍼 호출 한 줄로 발행한다.

**Tech Stack:** Spring Boot 4.0.7 (Java 21), Lombok, logstash-logback-encoder 7.4, Spring Data JPA + JdbcTemplate, Thymeleaf, Chart.js (CDN). Loki/Alloy/WebFlux/Docker Compose 변경 없음.

설계 출처: `docs/superpowers/specs/2026-06-27-data-pipeline-design.md` (이하 "스펙 §x").

## Global Constraints

- 로그 발행은 `com.kirozero.netzero.global.event.EventLogger.emit(...)` 한 줄로만. DTO/도메인 이벤트/listener를 만들지 않는다.
- `EventLogger.emit`은 내부에서 `try/catch(Exception)` 로 감싸 비즈니스 메서드를 절대 깨지 않는다(실패 시 WARN 한 줄).
- 로거 이름 `kiro.events`, INFO, `additivity=false` — 콘솔/일반 로그와 섞이지 않는다.
- `app` 값은 `kiro-backend` 고정, `env` 기본값 `dev`. 둘 다 Logback `customFields`로 자동 주입.
- 시간대는 전부 `Asia/Seoul`. `date` 필드는 KST 일자. 파일 롤링·배치 cron 모두 KST.
- 배치는 Spring Batch 프레임워크를 쓰지 않는다. `@Scheduled` + `@Transactional` + `JdbcTemplate`.
- 배치 1회 실행 = 하루치 1 트랜잭션. 실패 시 raw·집계 전부 롤백 → 같은 날짜 재실행 시 깨끗(멱등).
- 응답 DTO는 camelCase record 그대로 직렬화(전역 snake_case 설정 없음). 스펙 §7.2 예시 키와 일치시킨다.
- `/admin/**`은 인증 미적용(사내·로컬 접근 가정, 이번 스코프 밖).
- 자동 테스트는 작성하지 않는다. 검증은 배치 수동 트리거 + 페이지 렌더로 한다.
- 7개 테이블은 JPA 엔티티로 정의해 `ddl-auto=update`가 자동 생성하게 한다(별도 schema.sql 없음).

---

### Task 1: 의존성 + Logback 파일 적재 + 환경설정

**Files:**
- Modify: `build.gradle`
- Create: `src/main/resources/logback-spring.xml`
- Modify: `src/main/resources/application-dev.yml`
- Modify: `src/main/resources/application-prod.yml`

**Interfaces:**
- Consumes: 없음
- Produces:
  - SLF4J 로거 `kiro.events` (INFO) 호출 시 `${LOG_DIR}/events.jsonl`(활성 파일)에 JSON 한 줄 기록, 자정(KST) 롤링 시 `events-YYYY-MM-DD.jsonl`로 분리.
  - 모든 줄에 `ts`(RFC3339, KST), `app=kiro-backend`, `env=${APP_ENV:-dev}` 자동 포함.
  - `LOG_DIR` 환경변수: dev `./build/event-logs`, prod `/var/log/kiro`.

- [x] **Step 1: build.gradle 의존성 추가**

`dependencies` 블록에 한 줄 추가:

```groovy
    implementation 'net.logstash.logback:logstash-logback-encoder:7.4'
```

WebFlux/WebClient/Thymeleaf 추가 여부 확인: Thymeleaf는 Task 7에서 필요하므로 같은 블록에 추가:

```groovy
    implementation 'org.springframework.boot:spring-boot-starter-thymeleaf'
```

> JdbcTemplate은 `spring-boot-starter-data-jpa`(이미 있음)에 `spring-jdbc`가 transitive로 포함되어 별도 의존성 불필요.

- [x] **Step 2: logback-spring.xml 작성**

Create `src/main/resources/logback-spring.xml` (스펙 §4.2):

```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>

  <springProperty scope="context" name="appEnv" source="app.env" defaultValue="dev"/>

  <property name="LOG_DIR" value="${LOG_DIR:-/var/log/kiro}"/>

  <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
    <encoder>
      <pattern>%d{HH:mm:ss.SSS} %-5level %logger{36} - %msg%n</pattern>
    </encoder>
  </appender>

  <appender name="EVENT_FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
    <file>${LOG_DIR}/events.jsonl</file>
    <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
      <fileNamePattern>${LOG_DIR}/events-%d{yyyy-MM-dd, Asia/Seoul}.jsonl</fileNamePattern>
      <maxHistory>14</maxHistory>
    </rollingPolicy>
    <encoder class="net.logstash.logback.encoder.LogstashEncoder">
      <customFields>{"app":"kiro-backend","env":"${APP_ENV:-dev}"}</customFields>
      <timeZone>Asia/Seoul</timeZone>
      <fieldNames>
        <timestamp>ts</timestamp>
        <message>msg</message>
        <logger>[ignore]</logger>
        <thread>[ignore]</thread>
        <levelValue>[ignore]</levelValue>
        <level>[ignore]</level>
      </fieldNames>
      <timestampPattern>yyyy-MM-dd'T'HH:mm:ss.SSSXXX</timestampPattern>
    </encoder>
  </appender>

  <logger name="kiro.events" level="INFO" additivity="false">
    <appender-ref ref="EVENT_FILE"/>
  </logger>

  <root level="INFO">
    <appender-ref ref="CONSOLE"/>
  </root>

</configuration>
```

> `${APP_ENV:-dev}`는 OS 환경변수/시스템 프로퍼티에서 직접 치환된다. `springProperty` 줄은 제거해도 무방하나 남겨도 해롭지 않다. 핵심은 `customFields`의 `env` 값.

- [x] **Step 3: 프로파일에 LOG_DIR / APP_ENV 기본값 추가**

Modify `application-dev.yml` 끝에:

```yaml
app:
  env: dev
# LOG_DIR 기본값은 logback-spring.xml에서 /var/log/kiro 이므로
# dev 로컬에서는 실행 시 LOG_DIR=./build/event-logs 환경변수로 덮어쓴다.
```

> dev에서 `/var/log/kiro` 쓰기 권한이 없을 수 있으므로 로컬 실행은 `LOG_DIR=./build/event-logs ./gradlew bootRun` 형태로 띄운다(운영 메모 §9). prod는 `APP_ENV=prod`, `LOG_DIR=/var/log/kiro`를 컨테이너 env로 주입(이미 docker-compose에 `APP_ENV` 있음, `LOG_DIR`만 필요 시 추가).

Modify `application-prod.yml` 끝에:

```yaml
app:
  env: ${APP_ENV:prod}
```

- [x] **Step 4: 컴파일 + 의존성 확인**

Run:

```bash
./gradlew classes
./gradlew dependencies --configuration runtimeClasspath | grep -E "logstash|thymeleaf"
```

Expected: BUILD SUCCESSFUL, 두 라이브러리 표시.

- [ ] **Step 5: Commit**

```bash
git add build.gradle src/main/resources/logback-spring.xml \
        src/main/resources/application-dev.yml src/main/resources/application-prod.yml
git commit -m "feat(logging): kiro.events JSONL 파일 appender + logstash-encoder/thymeleaf 의존성"
```

---

### Task 2: EventLogger 발행 헬퍼

**Files:**
- Create: `src/main/java/com/kirozero/netzero/global/event/EventLogger.java`

**Interfaces:**
- Consumes: Task 1의 `kiro.events` 로거
- Produces:
  - `EventLogger.emit(String event, Map<String,Object> fields)` static 메서드. `fields`에 `event` 키를 추가해 `kiro.events`에 INFO 한 줄 기록.
  - 예외는 내부에서 삼키고 일반 로거에 WARN — 호출부로 전파하지 않음.

- [x] **Step 1: EventLogger 작성**

Create `src/main/java/com/kirozero/netzero/global/event/EventLogger.java` (스펙 §4.3):

```java
package com.kirozero.netzero.global.event;

import java.util.LinkedHashMap;
import java.util.Map;
import net.logstash.logback.argument.StructuredArguments;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class EventLogger {

    private static final Logger LOG = LoggerFactory.getLogger("kiro.events");
    private static final Logger FALLBACK = LoggerFactory.getLogger(EventLogger.class);

    private EventLogger() {
    }

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
```

- [x] **Step 2: 컴파일 + 스모크(선택, 임시 코드)**

`KiroBackendApplication.main` 끝에 임시로 한 줄 넣어 파일이 생기는지 확인 후 제거:

```java
com.kirozero.netzero.global.event.EventLogger.emit(
    "boot_smoke", java.util.Map.of("slot_id", 0, "date", "2026-06-27"));
```

Run:

```bash
LOG_DIR=./build/event-logs ./gradlew bootRun
```

Expected: `./build/event-logs/events.jsonl`에 `{"ts":"...","app":"kiro-backend","env":"dev","msg":"","slot_id":0,"date":"2026-06-27","event":"boot_smoke"}` 한 줄. 확인 후 임시 코드 제거.

- [ ] **Step 3: Commit**

```bash
git add src/main/java/com/kirozero/netzero/global/event/EventLogger.java
git commit -m "feat(events): EventLogger 발행 헬퍼 추가"
```

---

### Task 3: 비즈니스 서비스에 이벤트 발행 한 줄씩 추가

**Files:**
- Modify: `src/main/java/com/kirozero/netzero/domain/result/service/ConsumptionResultService.java`
- Modify: `src/main/java/com/kirozero/netzero/domain/session/service/SessionParticipationService.java`

**Interfaces:**
- Consumes: Task 2의 `EventLogger.emit`
- Produces: `events-YYYY-MM-DD.jsonl`에 적재되는 세 이벤트 — `session_completed`, `ingredient_used`, `participant_joined`. 필드는 스펙 §3.2~3.4 그대로.

- [x] **Step 1: `session_completed` + `ingredient_used` 발행 (ConsumptionResultService.createRecord)**

`createRecord(...)` 메서드에서 `slot.complete();` 직후(트랜잭션 종료 직전)에 추가한다. `record`, `slot`, `calculation`, `selectedMenu`, 저장된 item 리스트가 모두 스코프에 있다.

`participantCount`는 메서드 안에 변수가 없으므로 `request.items()` 와 무관하게 참여자 수가 필요하면 `sessionParticipantRepository.countBySlotId(slotId)`로 구한다. `menuName`/`menuType`은 `selectedMenu`에서 꺼낸다.

먼저 저장된 item 리스트를 변수로 받도록 기존 코드를 약간 조정:

```java
List<ConsumptionRecordItem> savedItems = consumptionRecordItemRepository.saveAll(
        request.items().stream()
                .map(item -> createItem(record, ingredientById.get(item.sessionIngredientId()), item.useRate()))
                .toList());
addRefundCashToParticipants(slotId, record.getRefundAmountPerUser());
slot.complete();

// --- 이벤트 발행 (스펙 §3.2 / §3.3) ---
int participantCount = (int) sessionParticipantRepository.countBySlotId(slotId);
BigDecimal totalLeftoverInput = savedItems.stream()
        .map(it -> ingredientById.get(it.getSessionIngredient().getId()).getEstimatedGrams())
        .reduce(BigDecimal.ZERO, BigDecimal::add);

EventLogger.emit("session_completed", java.util.Map.ofEntries(
        java.util.Map.entry("slot_id", slot.getId()),
        java.util.Map.entry("date", slot.getDate().toString()),
        java.util.Map.entry("place_name", slot.getPlaceName()),
        java.util.Map.entry("station_code", slot.getStationCode()),
        java.util.Map.entry("participant_count", participantCount),
        java.util.Map.entry("menu_name", selectedMenu.menuName()),
        java.util.Map.entry("menu_type", selectedMenu.menuType()),
        java.util.Map.entry("finished_food_rate", record.getFinishedFoodRate()),
        java.util.Map.entry("total_leftover_input_grams", totalLeftoverInput),
        java.util.Map.entry("total_leftover_used_grams", record.getTotalUsedGrams()),
        java.util.Map.entry("avg_ingredient_use_rate", record.getAvgIngredientUseRate()),
        java.util.Map.entry("estimated_food_waste_reduced_grams", record.getTotalUsedGrams()),
        java.util.Map.entry("estimated_carbon_saved_kgco2e", record.getEstimatedCarbonSavedKgco2e())
));

for (ConsumptionRecordItem it : savedItems) {
    SessionIngredient si = it.getSessionIngredient();
    BigDecimal input = si.getEstimatedGrams();
    BigDecimal used = it.getEstimatedUsedGrams();
    BigDecimal leftover = input.subtract(used).max(BigDecimal.ZERO);
    EventLogger.emit("ingredient_used", java.util.Map.ofEntries(
            java.util.Map.entry("slot_id", slot.getId()),
            java.util.Map.entry("date", slot.getDate().toString()),
            java.util.Map.entry("ingredient_name", si.getIngredient().getNameKo()),
            java.util.Map.entry("input_grams", input),
            java.util.Map.entry("used_grams", used),
            java.util.Map.entry("leftover_grams", leftover),
            java.util.Map.entry("use_rate", it.getUseRate())
    ));
}
```

> 실제 getter 이름은 엔티티에 맞게 확인/치환한다(`ConsumptionRecordItem.getEstimatedUsedGrams()`, `SessionIngredient.getEstimatedGrams()`, `Ingredient.getNameKo()`). `estimated_food_waste_reduced_grams`는 별도 산출값이 없으면 `total_used_grams`로 대체(스펙 표의 누적 음식물 집계에는 `total_leftover_used_grams`만 쓰이므로 영향 없음). import에 `com.kirozero.netzero.global.event.EventLogger` 추가.

- [x] **Step 2: `participant_joined` 발행 (SessionParticipationService)**

`SessionParticipant.create(...)`로 저장하는 줄(현재 ~51행) 직후에 추가:

```java
SessionParticipant participant = sessionParticipantRepository.save(
        SessionParticipant.create(slot, user, request.canPurchase())
);

EventLogger.emit("participant_joined", java.util.Map.of(
        "slot_id", slot.getId(),
        "date", slot.getDate().toString(),
        "user_id", user.getId()
));
```

import에 `EventLogger` 추가. `slot`, `user`가 스코프에 없으면 저장된 `participant`에서 `participant.getSlot()`, `participant.getUser()`로 꺼낸다.

- [ ] **Step 3: 빌드 + 발행 확인**

Run:

```bash
./gradlew classes
LOG_DIR=./build/event-logs ./gradlew bootRun
```

다른 터미널에서 슬롯 참여 1회 + 결과 제출 1회를 실제 API로 흘려보낸 뒤:

```bash
cat ./build/event-logs/events.jsonl
```

Expected: `participant_joined`, `session_completed`, `ingredient_used`(재료 수만큼) 줄이 보임. 각 줄에 `date`(YYYY-MM-DD), `slot_id` 포함.

> 비즈니스 API 흐름을 직접 못 돌리면 이 step은 해당 흐름을 띄울 수 있을 때 검증한다.

- [ ] **Step 4: Commit**

```bash
git add src/main/java/com/kirozero/netzero/domain/result/service/ConsumptionResultService.java \
        src/main/java/com/kirozero/netzero/domain/session/service/SessionParticipationService.java
git commit -m "feat(events): session_completed/ingredient_used/participant_joined 발행 추가"
```

---

### Task 4: RDS 테이블 엔티티 7종 + 집계 리포지토리

**Files:**
- Create: `domain/dashboard/entity/EventSessionCompletedRaw.java`
- Create: `domain/dashboard/entity/EventIngredientUsedRaw.java`
- Create: `domain/dashboard/entity/EventParticipantJoinedRaw.java`
- Create: `domain/dashboard/entity/DashboardDailyMetric.java`
- Create: `domain/dashboard/entity/DashboardDailyIngredient.java` (+ `DashboardDailyIngredientId`)
- Create: `domain/dashboard/entity/DashboardDailyPlace.java` (+ `DashboardDailyPlaceId`)
- Create: `domain/dashboard/entity/BatchJobHistory.java`
- Create: `domain/dashboard/repository/DashboardDailyMetricRepository.java`
- Create: `domain/dashboard/repository/DashboardDailyIngredientRepository.java`
- Create: `domain/dashboard/repository/DashboardDailyPlaceRepository.java`
- Create: `domain/dashboard/repository/BatchJobHistoryRepository.java`

(베이스 패키지 `com.kirozero.netzero.domain.dashboard`)

**Interfaces:**
- Consumes: 없음 (DDL은 `ddl-auto=update`가 엔티티에서 생성)
- Produces:
  - 7개 테이블: `event_session_completed_raw`, `event_ingredient_used_raw`, `event_participant_joined_raw`, `dashboard_daily_metric`, `dashboard_daily_ingredient`, `dashboard_daily_place`, `batch_job_history` (스펙 §6).
  - 집계 3종 + 히스토리 JPA 리포지토리.
  - `BatchJobStatus` enum: `RUNNING`, `SUCCESS`, `FAILED`, `EMPTY`.

- [x] **Step 1: raw 엔티티 3종**

raw 테이블은 배치가 `JdbcTemplate`으로 BULK INSERT(Task 5)하고 집계 SQL로만 읽는다. JPA 엔티티는 DDL 생성 용도. 컬럼은 스펙 §6.1 그대로.

`EventSessionCompletedRaw` 예시:

```java
package com.kirozero.netzero.domain.dashboard.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@NoArgsConstructor
@Table(name = "event_session_completed_raw",
       indexes = @Index(name = "idx_event_date", columnList = "event_date"))
public class EventSessionCompletedRaw {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "event_date", nullable = false) private LocalDate eventDate;
    @Column(name = "slot_id", nullable = false) private Long slotId;
    @Column(name = "place_name", nullable = false, length = 100) private String placeName;
    @Column(name = "station_code", length = 50) private String stationCode;
    @Column(name = "menu_name", length = 100) private String menuName;
    @Column(name = "menu_type", length = 20) private String menuType;
    @Column(name = "participant_count", nullable = false) private int participantCount;
    @Column(name = "finished_food_rate") private Integer finishedFoodRate;
    @Column(name = "total_leftover_input_grams", precision = 10, scale = 2) private BigDecimal totalLeftoverInputGrams;
    @Column(name = "total_leftover_used_grams", nullable = false, precision = 10, scale = 2) private BigDecimal totalLeftoverUsedGrams;
    @Column(name = "avg_ingredient_use_rate") private Integer avgIngredientUseRate;
    @Column(name = "estimated_food_waste_reduced_grams", precision = 10, scale = 2) private BigDecimal estimatedFoodWasteReducedGrams;
    @Column(name = "estimated_carbon_saved_kgco2e", nullable = false, precision = 10, scale = 3) private BigDecimal estimatedCarbonSavedKgco2e;
    @Column(name = "created_at", nullable = false) private LocalDateTime createdAt;
}
```

`EventIngredientUsedRaw`(컬럼: id, event_date, slot_id, ingredient_name VARCHAR(50), input_grams, used_grams NOT NULL, leftover_grams NOT NULL, use_rate, created_at)와 `EventParticipantJoinedRaw`(id, event_date, slot_id, user_id, created_at)도 같은 패턴으로. 모두 `idx_event_date` 인덱스.

- [x] **Step 2: 집계 엔티티 3종**

`DashboardDailyMetric` — PK `event_date` (스펙 §6.2):

```java
@Getter @Entity @NoArgsConstructor
@Table(name = "dashboard_daily_metric")
public class DashboardDailyMetric {
    @Id @Column(name = "event_date") private LocalDate eventDate;
    @Column(name = "completed_session_count", nullable = false) private int completedSessionCount;
    @Column(name = "participant_count", nullable = false) private int participantCount;
    @Column(name = "total_food_grams", nullable = false, precision = 12, scale = 2) private BigDecimal totalFoodGrams;
    @Column(name = "total_carbon_kgco2e", nullable = false, precision = 12, scale = 3) private BigDecimal totalCarbonKgco2e;
    @Column(name = "updated_at", nullable = false) private LocalDateTime updatedAt;
}
```

`DashboardDailyIngredient` — 복합 PK `(event_date, ingredient_name)`, `@IdClass(DashboardDailyIngredientId.class)`. 컬럼: total_used_grams, total_leftover_grams, updated_at + `idx_date(event_date)`.

`DashboardDailyPlace` — 복합 PK `(event_date, place_name)`, `@IdClass`. 컬럼: session_count, updated_at + `idx_date(event_date)`.

`@IdClass` 키 클래스는 `Serializable` + `equals/hashCode`(record로 만들면 자동). 예:

```java
public record DashboardDailyIngredientId(LocalDate eventDate, String ingredientName) implements java.io.Serializable {
    public DashboardDailyIngredientId() { this(null, null); }
}
```

> 집계 테이블은 배치가 `JdbcTemplate` UPSERT로 쓰고(Task 5) 어드민이 JPA/JPQL로 읽는다(Task 6). 엔티티의 setter는 만들지 않는다(읽기 전용).

- [x] **Step 3: BatchJobHistory 엔티티 + enum**

`BatchJobStatus` enum (`RUNNING`, `SUCCESS`, `FAILED`, `EMPTY`).

`BatchJobHistory` — PK `event_date` (스펙 §6.3): status(VARCHAR 16, `@Enumerated(STRING)`), session_rows, ingredient_rows, participant_rows, error_message(VARCHAR 500), started_at, finished_at.

- [x] **Step 4: 리포지토리 4종**

```java
public interface DashboardDailyMetricRepository extends JpaRepository<DashboardDailyMetric, LocalDate> {
    List<DashboardDailyMetric> findByEventDateGreaterThanEqualOrderByEventDate(LocalDate from);
}
```

`BatchJobHistoryRepository`는 `JpaRepository<BatchJobHistory, LocalDate>` + `Optional<LocalDate>` 형태의 마지막 성공일 조회용 메서드(또는 `@Query`로 `SELECT MAX(event_date) WHERE status='SUCCESS'`). 집계 ingredient/place 리포지토리는 TOP 조회용 `@Query`(스펙 §6.5 라/마/다)를 Task 6에서 채운다 — 여기선 빈 인터페이스로 생성.

- [x] **Step 5: 테이블 자동 생성 확인**

Run:

```bash
LOG_DIR=./build/event-logs ./gradlew bootRun
```

로컬 MySQL(`netzero3`)에서 7개 테이블이 생성됐는지 확인:

```bash
mysql -uroot -p12345678 netzero3 -e "SHOW TABLES LIKE '%event%'; SHOW TABLES LIKE 'dashboard%'; SHOW TABLES LIKE 'batch_job_history';"
```

Expected: 7개 테이블 표시. 복합 PK/인덱스도 `SHOW CREATE TABLE`로 확인.

- [ ] **Step 6: Commit**

```bash
git add src/main/java/com/kirozero/netzero/domain/dashboard/entity \
        src/main/java/com/kirozero/netzero/domain/dashboard/repository
git commit -m "feat(dashboard): raw/집계/히스토리 7개 테이블 엔티티 + 리포지토리"
```

---

### Task 5: 배치 (파일 → raw INSERT → 일별 집계 UPSERT)

**Files:**
- Modify: `src/main/java/com/kirozero/netzero/KiroBackendApplication.java` (`@EnableScheduling`)
- Create: `domain/dashboard/batch/LogIngestScheduler.java`
- Create: `domain/dashboard/batch/LogIngestJob.java`
- Create: `domain/dashboard/batch/EventLineParser.java`
- Create: `domain/dashboard/batch/ParsedEvent.java`
- Create: `domain/dashboard/batch/EventWhitelist.java`
- Create: `domain/dashboard/batch/DailyAggregator.java`
- Create: `domain/dashboard/batch/RawInsertRepository.java` (JdbcTemplate BULK INSERT)
- Create: `domain/dashboard/batch/BatchHistoryService.java` (멱등 가드 + 상태 기록)
- Modify: `application-dev.yml` / `application-prod.yml` (`LOG_DIR` 읽기용 프로퍼티)

**Interfaces:**
- Consumes: Task 1의 `events-YYYY-MM-DD.jsonl`, Task 4의 raw/집계/히스토리 테이블.
- Produces:
  - `LogIngestJob.run(LocalDate targetDate)` — public, `@Transactional`. 한 트랜잭션에서 멱등 가드 → 파싱·필터 → raw BULK INSERT → 집계 UPSERT → 히스토리 SUCCESS.
  - `LogIngestScheduler.runDaily()` — `@Scheduled(cron="0 0 3 * * *", zone="Asia/Seoul")` → `run(어제)`.
  - 멱등: 같은 날짜 status=SUCCESS면 즉시 종료.

- [x] **Step 1: @EnableScheduling 추가**

`KiroBackendApplication`에 `@EnableScheduling` 추가:

```java
@SpringBootApplication
@EnableScheduling
public class KiroBackendApplication { ... }
```

- [x] **Step 2: ParsedEvent + EventLineParser**

`ParsedEvent` — JSONL 한 줄을 파싱한 결과. `event()`, `field(String)`, `decimal(String)`, `intVal(String)`, `longVal(String)` 접근자 제공(내부 `Map<String,Object>` 또는 `JsonNode`).

`EventLineParser` — `parse(String line) → ParsedEvent`(깨진 줄이면 `null` + 일반 로거 WARN). `ObjectMapper`로 줄 단위 파싱.

```java
@Component
@RequiredArgsConstructor
public class EventLineParser {
    private static final Logger log = LoggerFactory.getLogger(EventLineParser.class);
    private final ObjectMapper objectMapper;

    public ParsedEvent parse(String line) {
        if (line == null || line.isBlank()) return null;
        try {
            JsonNode node = objectMapper.readTree(line);
            String event = node.path("event").asText(null);
            if (event == null) return null;
            return new ParsedEvent(event, node);
        } catch (Exception e) {
            log.warn("broken event log line skipped: {}", abbreviate(line), e);
            return null;
        }
    }
}
```

> `ObjectMapper`는 Spring Boot 기본 빈 주입.

- [x] **Step 3: EventWhitelist**

스펙 §5.5의 허용 이벤트 + 필수 필드 맵 한 군데:

```java
public final class EventWhitelist {
    private static final Map<String, List<String>> REQUIRED = Map.of(
        "session_completed", List.of("slot_id","date","total_leftover_used_grams",
            "estimated_carbon_saved_kgco2e","participant_count","place_name"),
        "ingredient_used", List.of("slot_id","date","ingredient_name","used_grams","leftover_grams"),
        "participant_joined", List.of("slot_id","date","user_id"));

    public static boolean isAllowed(String event) { return REQUIRED.containsKey(event); }
    public static boolean hasRequiredFields(ParsedEvent ev) {
        return REQUIRED.get(ev.event()).stream().allMatch(ev::has);
    }
    private EventWhitelist() {}
}
```

- [x] **Step 4: RawInsertRepository (JdbcTemplate BULK INSERT)**

`JdbcTemplate.batchUpdate`로 3종 raw 테이블에 청크(1,000건) 단위 INSERT. row 타입은 간단한 record 3개(`SessionCompletedRow`, `IngredientUsedRow`, `ParticipantJoinedRow`, 각각 `ParsedEvent`에서 `of(ev, date)` 팩토리). 예:

```java
@Repository
@RequiredArgsConstructor
public class RawInsertRepository {
    private final JdbcTemplate jdbc;

    public void bulkInsertSessions(List<SessionCompletedRow> rows) {
        jdbc.batchUpdate("""
            INSERT INTO event_session_completed_raw
              (event_date, slot_id, place_name, station_code, menu_name, menu_type,
               participant_count, finished_food_rate, total_leftover_input_grams,
               total_leftover_used_grams, avg_ingredient_use_rate,
               estimated_food_waste_reduced_grams, estimated_carbon_saved_kgco2e, created_at)
            VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,NOW())
            """, rows, 1000, (ps, r) -> { /* setX 13개 */ });
    }
    // bulkInsertIngredients / bulkInsertParticipants 동일 패턴
}
```

- [x] **Step 5: DailyAggregator (UPSERT SQL 3개)**

스펙 §6.4의 (a)(b)(c) SQL을 `NamedParameterJdbcTemplate`으로 실행. `:d`는 `targetDate`.

```java
@Component
@RequiredArgsConstructor
public class DailyAggregator {
    private final NamedParameterJdbcTemplate jdbc;

    public void refreshDailyMetrics(LocalDate d) { jdbc.update(METRIC_UPSERT, Map.of("d", d)); }
    public void refreshDailyIngredients(LocalDate d) {
        jdbc.update("DELETE FROM dashboard_daily_ingredient WHERE event_date = :d", Map.of("d", d));
        jdbc.update(INGREDIENT_INSERT, Map.of("d", d));
    }
    public void refreshDailyPlaces(LocalDate d) {
        jdbc.update("DELETE FROM dashboard_daily_place WHERE event_date = :d", Map.of("d", d));
        jdbc.update(PLACE_INSERT, Map.of("d", d));
    }
}
```

`METRIC_UPSERT`/`INGREDIENT_INSERT`/`PLACE_INSERT` 문자열은 스펙 §6.4 그대로 옮긴다(MySQL `ON DUPLICATE KEY UPDATE` / `DELETE`+`INSERT...SELECT...GROUP BY`).

- [x] **Step 6: BatchHistoryService (멱등 가드)**

`findStatus(date)`, `markRunning(date)`, `markSuccess(date, counts)`, `markEmpty(date)`, `markFailed(date, msg)`. `BatchJobHistoryRepository` 사용. 멱등 가드는 `findStatus == SUCCESS`면 true 반환.

> 주의: `LogIngestJob.run`이 `@Transactional`이라 같은 트랜잭션에서 FAILED를 기록하면 롤백된다. FAILED 기록은 **별도 트랜잭션**(`REQUIRES_NEW`)으로 남겨야 한다. `markFailed`에 `@Transactional(propagation = Propagation.REQUIRES_NEW)`를 건다. SUCCESS/EMPTY는 본 트랜잭션 안에서 기록.

- [x] **Step 7: LogIngestJob 본문**

스펙 §5.3 / §5.4 구조:

```java
@Component
@RequiredArgsConstructor
public class LogIngestJob {
    private final EventLineParser parser;
    private final RawInsertRepository rawRepo;
    private final DailyAggregator aggregator;
    private final BatchHistoryService history;

    @Value("${LOG_DIR:/var/log/kiro}") private String logDir;

    @Transactional
    public void run(LocalDate targetDate) {
        if (history.isAlreadySucceeded(targetDate)) return;        // 멱등 가드
        history.markRunning(targetDate);

        Path file = Path.of(logDir, "events-" + targetDate + ".jsonl");
        if (!Files.exists(file)) { history.markEmpty(targetDate); return; }

        Counts c = ingestRaw(file, targetDate);                    // 파싱+필터+BULK INSERT
        aggregator.refreshDailyMetrics(targetDate);
        aggregator.refreshDailyIngredients(targetDate);
        aggregator.refreshDailyPlaces(targetDate);
        history.markSuccess(targetDate, c);
    }
}
```

`ingestRaw`는 스펙 §5.4 그대로: `Files.lines`로 한 줄씩 → `parser.parse` → `EventWhitelist.isAllowed`/`hasRequiredFields`/`date==targetDate` 필터 → 타입별 row 리스트 적재 → `rawRepo.bulkInsert*`. 예외는 잡지 않고 던져서 트랜잭션 롤백 → 호출부(스케줄러)에서 `markFailed`.

스케줄러가 예외를 받아 FAILED 기록:

```java
@Component
@RequiredArgsConstructor
public class LogIngestScheduler {
    private final LogIngestJob job;
    private final BatchHistoryService history;

    @Scheduled(cron = "0 0 3 * * *", zone = "Asia/Seoul")
    public void runDaily() {
        LocalDate target = LocalDate.now(ZoneId.of("Asia/Seoul")).minusDays(1);
        runFor(target);
    }

    public void runFor(LocalDate target) {
        try {
            job.run(target);
        } catch (Exception e) {
            history.markFailed(target, e.getMessage());   // REQUIRES_NEW
        }
    }
}
```

> `runFor`는 Task 6의 수동 트리거(`POST /admin/api/batch/run`)에서도 재사용한다.

- [x] **Step 8: 검증 — 가짜 파일로 배치 실행**

`./build/event-logs/events-2026-06-26.jsonl`에 손으로 몇 줄 만들어 둔 뒤(또는 Task 3 흐름으로 생성된 어제 파일), 수동 트리거는 Task 6 이후이므로 여기서는 임시 `ApplicationRunner` 또는 테스트 엔드포인트 대신 **임시 main 호출**로 검증:

```java
// 임시: KiroBackendApplication 또는 CommandLineRunner에서 1회
scheduler.runFor(LocalDate.of(2026, 6, 26));
```

Run: `LOG_DIR=./build/event-logs ./gradlew bootRun`

Expected (MySQL):

```bash
mysql -uroot -p12345678 netzero3 -e "
  SELECT * FROM batch_job_history WHERE event_date='2026-06-26';
  SELECT * FROM dashboard_daily_metric WHERE event_date='2026-06-26';
  SELECT COUNT(*) FROM event_session_completed_raw WHERE event_date='2026-06-26';"
```

- `batch_job_history` status=SUCCESS, row 카운트 채워짐.
- `dashboard_daily_metric` 한 행, 합계 맞음.
- 같은 날짜로 한 번 더 `runFor` → 멱등 가드로 즉시 종료(중복 INSERT 없음).

확인 후 임시 호출 제거.

- [ ] **Step 9: Commit**

```bash
git add src/main/java/com/kirozero/netzero/domain/dashboard/batch \
        src/main/java/com/kirozero/netzero/KiroBackendApplication.java
git commit -m "feat(batch): 일자 로그 파일 → raw INSERT → 일별 집계 UPSERT 배치"
```

---

### Task 6: 어드민 조회 API (Service + DTO + Controller + 수동 트리거)

**Files:**
- Create: `domain/dashboard/dto/ImpactSummaryResponse.java`
- Create: `domain/dashboard/dto/DailyTrendResponse.java`
- Create: `domain/dashboard/dto/TopItemResponse.java`
- Create: `domain/dashboard/service/DashboardQueryService.java`
- Modify: `domain/dashboard/repository/DashboardDailyMetricRepository.java` (집계 조회 `@Query`)
- Modify: `domain/dashboard/repository/DashboardDailyIngredientRepository.java` (TOP `@Query`)
- Modify: `domain/dashboard/repository/DashboardDailyPlaceRepository.java` (TOP `@Query`)
- Create: `domain/dashboard/controller/DashboardApiController.java`
- Create: `domain/dashboard/controller/DashboardController.java`

**Interfaces:**
- Consumes: Task 4 리포지토리/엔티티, Task 5 `LogIngestScheduler.runFor`.
- Produces 6개 HTTP 엔드포인트(스펙 §7.2):
  - GET `/admin/dashboard` → HTML (Task 7 템플릿)
  - GET `/admin/api/metrics/impact` → `ImpactSummaryResponse`
  - GET `/admin/api/metrics/daily-trend` → `DailyTrendResponse`
  - GET `/admin/api/metrics/top-places` → `List<TopItemResponse>`
  - GET `/admin/api/metrics/top-used-ingredients` → `List<TopItemResponse>`
  - GET `/admin/api/metrics/top-leftover-ingredients` → `List<TopItemResponse>`
  - POST `/admin/api/batch/run?date=YYYY-MM-DD` → 수동 트리거(데모)
- Produces DTO record(camelCase, 스펙 §7.2 키와 일치):
  - `ImpactSummaryResponse(double totalFoodProcessedKg, double totalCarbonSavedKgco2e, long totalParticipants, long totalCompletedSessions, double treeEquivalent)`
  - `DailyTrendResponse(List<String> labels, Series series)` / `Series(List<Long> sessions, List<Long> participants, List<Double> foodGrams, List<Double> carbonKg)`
  - `TopItemResponse(String name, double value)`

- [x] **Step 1: DTO 3종 작성**

스펙 §7.2 응답 예시에 맞춰 record로. `treeEquivalent`는 서비스에서 `totalCarbonSavedKgco2e / 22` 반올림 둘째자리.

- [x] **Step 2: 리포지토리 조회 메서드**

스펙 §6.5 SQL을 JPQL/`@Query`로:

- `DashboardDailyMetricRepository`:
  - 누적 합산(가): `@Query` projection 또는 `findByEventDateGreaterThanEqual...`로 30일치 받아 서비스에서 합산.
  - 일별 추이(나): `findByEventDateGreaterThanEqualOrderByEventDate(today.minusDays(4))`.
- `DashboardDailyIngredientRepository`: TOP used(라)/leftover(마) — `@Query`로 `SUM(...) GROUP BY ingredient_name ORDER BY ... DESC` + `Pageable`(LIMIT 5). projection은 `TopItemResponse` 또는 `Object[]`.
- `DashboardDailyPlaceRepository`: TOP places(다) — `SUM(session_count) GROUP BY place_name`.

> 네이티브 쿼리(`nativeQuery=true`)로 스펙 §6.5 SQL을 거의 그대로 써도 된다. `CURDATE() - INTERVAL 30 DAY`는 파라미터 `:from = today.minusDays(29)`로 바꿔 JPQL과 호환.

- [x] **Step 3: DashboardQueryService**

메서드 5개 + 마지막 성공일. 일별 추이는 빈 날짜를 0으로 채워 항상 5개 라벨 정렬(스펙 §6.5 나, §7.3):

```java
@Service
@RequiredArgsConstructor
public class DashboardQueryService {
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final double KG_PER_TREE = 22.0;

    public ImpactSummaryResponse impact() { /* 30일 합산 + treeEquivalent */ }
    public DailyTrendResponse dailyTrend() { /* 최근 5일, 빈 날 0 채움 */ }
    public List<TopItemResponse> topPlaces() { ... }
    public List<TopItemResponse> topUsedIngredients() { ... }
    public List<TopItemResponse> topLeftoverIngredients() { ... }
    public LocalDate findLastSuccessDate() { /* batch_job_history MAX(event_date) status=SUCCESS */ }
}
```

`totalFoodProcessedKg`는 `total_food_grams / 1000` 환산.

- [x] **Step 4: DashboardApiController**

`@RestController @RequestMapping("/admin/api")`. metrics 5개 GET + `POST /batch/run`:

```java
@PostMapping("/batch/run")
public Map<String, Object> runBatch(@RequestParam @DateTimeFormat(iso = DATE) LocalDate date) {
    scheduler.runFor(date);
    return Map.of("triggered", date.toString());
}
```

- [x] **Step 5: DashboardController (페이지 셸)**

```java
@Controller
@RequiredArgsConstructor
public class DashboardController {
    private final DashboardQueryService queryService;

    @GetMapping("/admin/dashboard")
    public String dashboard(Model model) {
        model.addAttribute("lastUpdatedDate", queryService.findLastSuccessDate());
        return "admin/dashboard";
    }
}
```

- [x] **Step 6: 예외 매핑 (선택)**

조회 실패는 기존 `ApiExceptionHandler`에 일반 `Exception` 핸들러를 추가하지 않는다(전역 영향 큼). 대신 대시보드 API는 실패 시 Spring 기본 500을 반환하고, 프론트가 `Promise.allSettled`로 카드별 처리(스펙 §8). 별도 작업 없음.

- [x] **Step 7: 검증 — API 응답 확인**

Run: `LOG_DIR=./build/event-logs ./gradlew bootRun` (Task 5에서 `2026-06-26` 집계가 있다고 가정)

```bash
curl -s "http://localhost:8080/admin/api/metrics/impact" | jq
curl -s "http://localhost:8080/admin/api/metrics/daily-trend" | jq
curl -s "http://localhost:8080/admin/api/metrics/top-used-ingredients" | jq
curl -s -X POST "http://localhost:8080/admin/api/batch/run?date=2026-06-26" | jq
```

Expected: 스펙 §7.2 형태 JSON. `daily-trend`의 `labels`는 5개, 데이터 없는 날은 0.

- [ ] **Step 8: Commit**

```bash
git add src/main/java/com/kirozero/netzero/domain/dashboard/dto \
        src/main/java/com/kirozero/netzero/domain/dashboard/service \
        src/main/java/com/kirozero/netzero/domain/dashboard/repository \
        src/main/java/com/kirozero/netzero/domain/dashboard/controller
git commit -m "feat(dashboard): 집계 조회 서비스 + 5개 메트릭 API + 수동 배치 트리거"
```

---

### Task 7: 어드민 페이지 (Thymeleaf + admin.css + Chart.js)

**Files:**
- Create: `src/main/resources/templates/admin/dashboard.html`
- Create: `src/main/resources/static/admin/admin.css`

**Interfaces:**
- Consumes: Task 6의 6개 엔드포인트, `lastUpdatedDate` 모델 속성.
- Produces: `/admin/dashboard` 렌더 페이지(KPI 카드 5 + 5일 추이 차트 + TOP 표 3). 자동 새로고침 없음.

- [x] **Step 1: admin.css 작성**

참고 디자인(스펙 §7 — Django admin 톤: 네이비 헤더 `#417690`, 노란 브랜드 `#f5dd5d`, 밝은 그레이 배경, `.module results` 카드, 표 중심). 스펙 §7.5의 `.kpi-grid`/`.kpi-card`/`.chart-card`/`.analytics-grid` 클래스 포함.

- [x] **Step 2: dashboard.html 작성**

스펙 §7.4 구성: `.admin-header` → `.breadcrumbs`(Last updated: `${lastUpdatedDate}`) → `main.content`(content-header / `.kpi-grid` 5칸 / "Last 5 days trend" Chart.js 영역차트 듀얼 Y축 / "Top places" 표 / `.analytics-grid` 식재료 TOP 2표).

프론트 동작(스펙 §7.4): 로드 시 `Promise.allSettled([impact, dailyTrend, topPlaces, topUsed, topLeftover])`, 각 fetch 독립 처리(실패 카드만 "Failed to load"). Chart.js CDN 한 줄.

```html
<!doctype html>
<html xmlns:th="http://www.thymeleaf.org">
<head>
  <meta charset="utf-8"/>
  <title>Impact Dashboard</title>
  <link rel="stylesheet" th:href="@{/admin/admin.css}"/>
  <script src="https://cdn.jsdelivr.net/npm/chart.js"></script>
</head>
<body>
  <!-- header / breadcrumbs (lastUpdatedDate) / kpi-grid / chart / tables -->
  <script>
    const $ = (id) => document.getElementById(id);
    async function load() {
      const ep = {
        impact: '/admin/api/metrics/impact',
        trend:  '/admin/api/metrics/daily-trend',
        places: '/admin/api/metrics/top-places',
        used:   '/admin/api/metrics/top-used-ingredients',
        left:   '/admin/api/metrics/top-leftover-ingredients',
      };
      const [impact, trend, places, used, left] = await Promise.allSettled(
        Object.values(ep).map(u => fetch(u).then(r => { if (!r.ok) throw 0; return r.json(); })));
      // 각 결과 status==='fulfilled' 면 렌더, 'rejected' 면 'Failed to load'
    }
    load();
  </script>
</body>
</html>
```

- [ ] **Step 3: 검증 — 브라우저 렌더**

Run: `LOG_DIR=./build/event-logs ./gradlew bootRun` → 브라우저 `http://localhost:8080/admin/dashboard`.

Expected: KPI 카드 5개 숫자, 5일 추이 차트, TOP 표 3개가 보임. 데이터가 비어도 0/빈 표로 깨지지 않음. 한 API를 일부러 막아도 그 카드만 "Failed to load".

- [ ] **Step 4: Commit**

```bash
git add src/main/resources/templates/admin/dashboard.html \
        src/main/resources/static/admin/admin.css
git commit -m "feat(dashboard): 어드민 임팩트 대시보드 페이지 + admin.css"
```

---

## 완료 기준

- `events-YYYY-MM-DD.jsonl`에 세 이벤트가 적재된다(Task 3).
- `runFor(어제)` 1회로 raw 3종 INSERT + 집계 3종 UPSERT + 히스토리 SUCCESS, 재실행 시 멱등(Task 5).
- `/admin/dashboard`가 KPI·추이·TOP을 렌더한다(Task 6~7).
- Loki/Alloy/WebFlux/docker-compose 변경 없음.

## 스코프 밖 (스펙 §10)

인증/인가, 알림, Prometheus, 다환경 분리, 자동 새로고침, 실시간 스트리밍, 누적 고유 참여자 수, 테스트 자동화, 배치 실패 알림·리트라이 큐·데드레터.
