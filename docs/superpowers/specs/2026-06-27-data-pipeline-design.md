# 데이터 파이프라인 설계 (파일 로그 → 일 배치 → RDS 집계 → 어드민)

작성일: 2026-06-27
팀: KiroZero / 서비스: 냉장고 반상회 MVP

## 1. 목표

Spring 백엔드에서 발생하는 도메인 이벤트를 일자별 JSON Lines 파일로
적재하고, 매일 새벽 03:00 KST에 Spring `@Scheduled` 배치가 어제 파일을
한 번 읽어 화이트리스트 이벤트만 RDS(MySQL)의 raw 테이블에 INSERT 한 뒤,
같은 트랜잭션에서 그 날짜 분만 GROUP BY 해 일별 집계 테이블을 UPSERT 한다.
어드민 대시보드는 이 일별 집계 테이블만 SELECT 해서 그린다.

비목표:

- Loki/Alloy/실시간 LogQL 파이프라인
- DynamoDB·별도 분석 DB·웨어하우스
- 인증/인가, 알림, 자동 새로고침, 실시간 스트리밍
- 누적 고유(distinct) 참여자 수
- 도메인 이벤트(`ApplicationEventPublisher`)/`@TransactionalEventListener`
  기반 발행 — 비즈니스 서비스 메서드 안에서 헬퍼 한 줄로 끝낸다.

## 2. 전체 흐름

```
Spring 비즈니스 서비스
  └─ EventLogger.emit("session_completed", Map.of(...))   ← 메서드 안 한 줄
        │
        ▼ Logback RollingFileAppender + LogstashEncoder
   ${LOG_DIR}/events-YYYY-MM-DD.jsonl                    (앱 호스트 로컬)
        │
        ▼ 매일 03:00 KST · @Scheduled
LogIngestScheduler → LogIngestJob (한 트랜잭션)
   (1) batch_job_history 멱등 가드
   (2) 어제 날짜 파일 한 줄씩 파싱
   (3) EventWhitelist 통과한 줄만 raw 3종에 BULK INSERT
   (4) 같은 날짜 분 GROUP BY → 일별 집계 3종 UPSERT
   (5) batch_job_history status='SUCCESS'
        │
        ▼
RDS MySQL
   raw : event_session_completed_raw / event_ingredient_used_raw /
         event_participant_joined_raw
   agg : dashboard_daily_metric / dashboard_daily_ingredient /
         dashboard_daily_place
   meta: batch_job_history
        │
        ▲ 짧은 SELECT/SUM (집계 테이블만)
DashboardApiController · DashboardQueryService
        │
        ▼ JSON
어드민 페이지 (/admin/dashboard) — fetch + Chart.js
```

핵심 차이점(이전 안 대비):

- Loki·Alloy·docker-compose 변경 전부 제거.
- LogQL 전부 제거 → 일반 JPA/SQL.
- 대시보드는 운영 DB의 "작은 집계 테이블"만 조회(수십 행 수준).

## 3. 로그 스키마

### 3.1 공통 필드

```json
{
  "ts": "2026-06-27T10:30:45.123+09:00",
  "app": "kiro-backend",
  "env": "dev",
  "event": "<이벤트명>"
}
```

`app`, `env`는 Logback `LogstashEncoder.customFields`로 매 줄 자동 주입.
`ts`는 RFC3339 (`Asia/Seoul`). 배치는 KST 일자 비교에만 사용.
`event` 값이 화이트리스트의 키 — 이 값이 없거나 모르는 값이면 배치가 폐기.

### 3.2 `session_completed`

발생: `ConsumptionRecord` 저장이 끝난 서비스 메서드의 마지막 줄.

| 필드 | 타입 | 비고 |
|---|---|---|
| slot_id | int | |
| date | string (YYYY-MM-DD) | KST 기준 슬롯 날짜 |
| place_name | string | TOP 인기장소 집계용 |
| station_code | string | |
| participant_count | int | |
| menu_name | string | |
| menu_type | enum | GENERAL / LOW_CARBON |
| finished_food_rate | int | 0–100 |
| total_leftover_input_grams | number | |
| total_leftover_used_grams | number | 누적 음식물(g) 집계용 |
| avg_ingredient_use_rate | int | 0–100 |
| estimated_food_waste_reduced_grams | number | |
| estimated_carbon_saved_kgco2e | number | 누적 탄소 집계용 |

### 3.3 `ingredient_used`

발생: 같은 서비스 메서드 안에서 `ConsumptionRecordItem` 저장 직후, 재료별로
한 줄씩 발행.

| 필드 | 타입 |
|---|---|
| slot_id | int |
| date | string (YYYY-MM-DD) |
| ingredient_name | string |
| input_grams | number |
| used_grams | number |
| leftover_grams | number |
| use_rate | int |

### 3.4 `participant_joined`

발생: `SessionParticipant` 저장 메서드 마지막 줄.

| 필드 | 타입 |
|---|---|
| slot_id | int |
| date | string (YYYY-MM-DD) |
| user_id | int |

### 3.5 발행 규칙

- 로거 이름은 `kiro.events`, INFO, `additivity=false` — 콘솔/일반 로그에
  안 섞임.
- 메시지 본문은 빈 문자열, 필드는 전부
  `StructuredArguments.entries(Map.of(...))` 로 주입.
- 발행 호출은 `try/catch (Exception)` 로 감싸 비즈니스 메서드를 절대
  깨지 않음. 실패 시 일반 로거에 WARN 한 줄.
- DTO / 도메인 이벤트 / Listener 만들지 않음. 서비스 메서드 안에서
  `EventLogger.emit(...)` 한 줄.

## 4. 로그 파일 적재 (Logback)

### 4.1 의존성

`build.gradle`:

- `net.logstash.logback:logstash-logback-encoder:7.4`

다른 의존성 추가 없음(WebFlux/WebClient 불필요).

### 4.2 `src/main/resources/logback-spring.xml` (신규)

- 콘솔 appender(기존 사람이 읽는 포맷) 그대로 유지, `root` 에 연결.
- `EVENT_FILE` appender 신규:
  - `RollingFileAppender`
  - 활성 파일: `${LOG_DIR:-/var/log/kiro}/events.jsonl`
  - 롤링 패턴: `${LOG_DIR:-/var/log/kiro}/events-%d{yyyy-MM-dd, Asia/Seoul}.jsonl`
  - `TimeBasedRollingPolicy`, 자정(KST) 기준 일자 롤링
  - `maxHistory=14` — 14일 지난 파일 자동 삭제
  - 인코더는 `LogstashEncoder`,
    `customFields={"app":"kiro-backend","env":"${APP_ENV:-dev}"}`,
    `timeZone=Asia/Seoul`
- 로거 `kiro.events`: `level=INFO`, `additivity=false`,
  `EVENT_FILE` 한 군데만 연결.

### 4.3 발행 헬퍼

`com.kirozero.netzero.global.event.EventLogger` 한 파일:

```java
public final class EventLogger {
    private static final Logger LOG = LoggerFactory.getLogger("kiro.events");
    public static void emit(String event, Map<String, Object> fields) {
        try {
            Map<String, Object> m = new LinkedHashMap<>(fields);
            m.put("event", event);
            LOG.info("", StructuredArguments.entries(m));
        } catch (Exception e) {
            LoggerFactory.getLogger(EventLogger.class)
                .warn("event emit failed: {}", event, e);
        }
    }
    private EventLogger() {}
}
```

사용:

```java
EventLogger.emit("session_completed", Map.of(
    "slot_id", slotId,
    "date", date.toString(),
    "place_name", placeName,
    /* ... 3.2 표의 나머지 필드 ... */
));
```

### 4.4 환경 설정

- `application-dev.yml`: `LOG_DIR=./build/event-logs`, `APP_ENV=dev`
- `application-prod.yml`: `LOG_DIR=/var/log/kiro`, `APP_ENV=prod`
- 배치도 같은 `LOG_DIR` 환경변수를 읽어 파일 경로 조립.

### 4.5 회전·삭제 책임

| 책임 | 누가 |
|---|---|
| 일자별 파일 분리 | Logback `TimeBasedRollingPolicy` |
| 14일 지난 파일 삭제 | Logback `maxHistory` |
| 어제 파일 1회 처리 | Spring 배치 (§5) |
| 처리 완료 표시 | `batch_job_history` (§6.4) |

## 5. 배치

### 5.1 클래스 구조

```
com.kirozero.netzero.domain.dashboard.batch
  ├─ LogIngestScheduler.java     @Scheduled 진입점 1메서드
  ├─ LogIngestJob.java           run(LocalDate) 본문
  ├─ EventLineParser.java        JSONL 한 줄 → ParsedEvent
  ├─ EventWhitelist.java         허용 이벤트 + 필수 필드 정의
  └─ DailyAggregator.java        일별 집계 UPSERT SQL 3개 실행
```

Spring Batch 프레임워크 미사용 — `@Scheduled` + 트랜잭션 + `JdbcTemplate`
로 충분. `@EnableScheduling` 은 `KiroBackendApplication` 또는 별도
`SchedulingConfig` 에 한 번 선언.

### 5.2 진입점

```java
@Component
@RequiredArgsConstructor
public class LogIngestScheduler {
    private final LogIngestJob job;

    @Scheduled(cron = "0 0 3 * * *", zone = "Asia/Seoul")
    public void runDaily() {
        job.run(LocalDate.now(ZoneId.of("Asia/Seoul")).minusDays(1));
    }
}
```

### 5.3 잡 본문

```java
@Component
@RequiredArgsConstructor
public class LogIngestJob {

    @Transactional
    public void run(LocalDate targetDate) {
        // (1) 멱등 가드
        if (historyRepo.findStatus(targetDate) == SUCCESS) return;
        historyRepo.upsert(targetDate, RUNNING);

        Path file = resolveFile(targetDate);  // ${LOG_DIR}/events-YYYY-MM-DD.jsonl
        if (!Files.exists(file)) {
            historyRepo.upsert(targetDate, EMPTY);
            return;
        }

        // (2~3) 파싱 + 필터 + raw INSERT
        Counts c = ingestRaw(file, targetDate);

        // (4) 일별 집계 UPSERT (어제 일자 한 건)
        aggregator.refreshDailyMetrics(targetDate);
        aggregator.refreshDailyIngredients(targetDate);
        aggregator.refreshDailyPlaces(targetDate);

        // (5) 성공 기록
        historyRepo.upsert(targetDate, SUCCESS, c);
    }
}
```

트랜잭션 단위: 하루치 전체를 한 트랜잭션. 실패하면 raw·집계 모두 롤백
→ 같은 날짜로 재실행 시 깨끗.

### 5.4 파일 읽기 + 필터 + raw INSERT

```java
private Counts ingestRaw(Path file, LocalDate date) {
    List<SessionCompletedRow> sc = new ArrayList<>();
    List<IngredientUsedRow>   iu = new ArrayList<>();
    List<ParticipantJoinedRow> pj = new ArrayList<>();

    try (var lines = Files.lines(file, StandardCharsets.UTF_8)) {
        lines.forEach(line -> {
            ParsedEvent ev = parser.parse(line);     // 깨진 줄은 null + WARN
            if (ev == null) return;
            if (!EventWhitelist.isAllowed(ev.event())) return;
            if (!EventWhitelist.hasRequiredFields(ev)) return;
            if (!date.toString().equals(ev.field("date"))) return;  // 자정경계 안전망

            switch (ev.event()) {
                case "session_completed"  -> sc.add(SessionCompletedRow.of(ev));
                case "ingredient_used"    -> iu.add(IngredientUsedRow.of(ev));
                case "participant_joined" -> pj.add(ParticipantJoinedRow.of(ev));
            }
        });
    }

    // 1,000건 단위 청크 BULK INSERT
    rawSessionRepo.bulkInsert(sc);
    rawIngredientRepo.bulkInsert(iu);
    rawParticipantRepo.bulkInsert(pj);

    return new Counts(sc.size(), iu.size(), pj.size());
}
```

### 5.5 화이트리스트 (어떤 로그만 INSERT 할지)

`EventWhitelist` 상수 맵 한 군데:

| event | 필수 필드 |
|---|---|
| `session_completed` | `slot_id`, `date`, `total_leftover_used_grams`, `estimated_carbon_saved_kgco2e`, `participant_count`, `place_name` |
| `ingredient_used` | `slot_id`, `date`, `ingredient_name`, `used_grams`, `leftover_grams` |
| `participant_joined` | `slot_id`, `date`, `user_id` |

규칙:

- `event` 값이 위 3개가 아니면 폐기.
- 필수 필드 누락 → 폐기.
- `date` ≠ `targetDate` → 폐기(자정 경계 안전망).
- 깨진 JSON 줄 → WARN 한 줄 + 폐기. 배치는 안 깨짐.

### 5.6 재실행·멱등

- 같은 `targetDate` 두 번 실행 → 멱등 가드에서 즉시 종료.
- 강제 재실행: `batch_job_history` 해당 행 삭제 또는 status 변경 후
  `/admin/api/batch/run?date=YYYY-MM-DD` (§7.2) 호출.

## 6. RDS 테이블

MySQL 기준. 총 7개: raw 3 + 집계 3 + 히스토리 1.

### 6.1 raw 테이블

**`event_session_completed_raw`**

| 컬럼 | 타입 | 비고 |
|---|---|---|
| id | BIGINT PK AI | |
| event_date | DATE NOT NULL | KST 일자 |
| slot_id | BIGINT NOT NULL | |
| place_name | VARCHAR(100) NOT NULL | |
| station_code | VARCHAR(50) | |
| menu_name | VARCHAR(100) | |
| menu_type | VARCHAR(20) | |
| participant_count | INT NOT NULL | |
| finished_food_rate | INT | |
| total_leftover_input_grams | DECIMAL(10,2) | |
| total_leftover_used_grams | DECIMAL(10,2) NOT NULL | |
| avg_ingredient_use_rate | INT | |
| estimated_food_waste_reduced_grams | DECIMAL(10,2) | |
| estimated_carbon_saved_kgco2e | DECIMAL(10,3) NOT NULL | |
| created_at | DATETIME NOT NULL DEFAULT NOW() | |

인덱스: `idx_event_date (event_date)`.

**`event_ingredient_used_raw`**

| 컬럼 | 타입 |
|---|---|
| id | BIGINT PK AI |
| event_date | DATE NOT NULL |
| slot_id | BIGINT NOT NULL |
| ingredient_name | VARCHAR(50) NOT NULL |
| input_grams | DECIMAL(10,2) |
| used_grams | DECIMAL(10,2) NOT NULL |
| leftover_grams | DECIMAL(10,2) NOT NULL |
| use_rate | INT |
| created_at | DATETIME NOT NULL DEFAULT NOW() |

인덱스: `idx_event_date (event_date)`.

**`event_participant_joined_raw`**

| 컬럼 | 타입 |
|---|---|
| id | BIGINT PK AI |
| event_date | DATE NOT NULL |
| slot_id | BIGINT NOT NULL |
| user_id | BIGINT NOT NULL |
| created_at | DATETIME NOT NULL DEFAULT NOW() |

인덱스: `idx_event_date (event_date)`.

### 6.2 일별 집계 테이블

**`dashboard_daily_metric`** — 누적 카드 + 일별 추이 소스

| 컬럼 | 타입 | 비고 |
|---|---|---|
| event_date | DATE PK | |
| completed_session_count | INT NOT NULL | |
| participant_count | INT NOT NULL | 이벤트 누적(중복 포함) |
| total_food_grams | DECIMAL(12,2) NOT NULL | leftover_used 합 |
| total_carbon_kgco2e | DECIMAL(12,3) NOT NULL | |
| updated_at | DATETIME NOT NULL | |

**`dashboard_daily_ingredient`** — TOP 식재료 2종 카드 소스

| 컬럼 | 타입 |
|---|---|
| event_date | DATE NOT NULL |
| ingredient_name | VARCHAR(50) NOT NULL |
| total_used_grams | DECIMAL(12,2) NOT NULL |
| total_leftover_grams | DECIMAL(12,2) NOT NULL |
| updated_at | DATETIME NOT NULL |
| PK | (event_date, ingredient_name) |

인덱스: `idx_date (event_date)`.

**`dashboard_daily_place`** — TOP 인기장소 카드 소스

| 컬럼 | 타입 |
|---|---|
| event_date | DATE NOT NULL |
| place_name | VARCHAR(100) NOT NULL |
| session_count | INT NOT NULL |
| updated_at | DATETIME NOT NULL |
| PK | (event_date, place_name) |

인덱스: `idx_date (event_date)`.

### 6.3 배치 히스토리

**`batch_job_history`**

| 컬럼 | 타입 |
|---|---|
| event_date | DATE PK |
| status | VARCHAR(16) NOT NULL |
| session_rows | INT |
| ingredient_rows | INT |
| participant_rows | INT |
| error_message | VARCHAR(500) |
| started_at | DATETIME NOT NULL |
| finished_at | DATETIME |

`status` 값: `RUNNING`, `SUCCESS`, `FAILED`, `EMPTY`.

대시보드 페이지 상단의 "Last updated" 표시도 이 테이블의
`MAX(event_date) WHERE status='SUCCESS'` 로 채움.

### 6.4 집계 UPSERT 쿼리 (§5.3의 4단계)

타깃 날짜를 `:d` 라고 할 때.

**(a) `dashboard_daily_metric`**

```sql
INSERT INTO dashboard_daily_metric
  (event_date, completed_session_count, participant_count,
   total_food_grams, total_carbon_kgco2e, updated_at)
SELECT
  :d,
  (SELECT COUNT(*) FROM event_session_completed_raw WHERE event_date = :d),
  (SELECT COUNT(*) FROM event_participant_joined_raw WHERE event_date = :d),
  COALESCE((SELECT SUM(total_leftover_used_grams)    FROM event_session_completed_raw WHERE event_date = :d), 0),
  COALESCE((SELECT SUM(estimated_carbon_saved_kgco2e) FROM event_session_completed_raw WHERE event_date = :d), 0),
  NOW()
ON DUPLICATE KEY UPDATE
  completed_session_count = VALUES(completed_session_count),
  participant_count       = VALUES(participant_count),
  total_food_grams        = VALUES(total_food_grams),
  total_carbon_kgco2e     = VALUES(total_carbon_kgco2e),
  updated_at              = VALUES(updated_at);
```

**(b) `dashboard_daily_ingredient`** — 해당 날짜 행 삭제 후 GROUP BY 결과 삽입

```sql
DELETE FROM dashboard_daily_ingredient WHERE event_date = :d;

INSERT INTO dashboard_daily_ingredient
  (event_date, ingredient_name, total_used_grams, total_leftover_grams, updated_at)
SELECT event_date, ingredient_name, SUM(used_grams), SUM(leftover_grams), NOW()
FROM event_ingredient_used_raw
WHERE event_date = :d
GROUP BY event_date, ingredient_name;
```

**(c) `dashboard_daily_place`**

```sql
DELETE FROM dashboard_daily_place WHERE event_date = :d;

INSERT INTO dashboard_daily_place
  (event_date, place_name, session_count, updated_at)
SELECT event_date, place_name, COUNT(*), NOW()
FROM event_session_completed_raw
WHERE event_date = :d
GROUP BY event_date, place_name;
```

세 쿼리 모두 "하루치 한 날짜만 다시 굽는" 구조 → 멱등.

### 6.5 어드민 조회 쿼리

**(가) 누적 임팩트 (30일 합산)**

```sql
SELECT
  COALESCE(SUM(completed_session_count), 0) AS sessions,
  COALESCE(SUM(participant_count), 0)       AS participants,
  COALESCE(SUM(total_food_grams), 0)        AS food_grams,
  COALESCE(SUM(total_carbon_kgco2e), 0)     AS carbon_kg
FROM dashboard_daily_metric
WHERE event_date >= CURDATE() - INTERVAL 30 DAY;
```

`treeEquivalent = carbon_kg / 22` (컨트롤러 계산).

**(나) 최근 5일 일별 추이**

```sql
SELECT event_date,
       completed_session_count, participant_count,
       total_food_grams, total_carbon_kgco2e
FROM dashboard_daily_metric
WHERE event_date >= CURDATE() - INTERVAL 4 DAY
ORDER BY event_date;
```

빈 날짜는 서비스가 0으로 채워 5개 라벨 정렬.

**(다) 인기 장소 TOP 5 (30일)**

```sql
SELECT place_name, SUM(session_count) AS cnt
FROM dashboard_daily_place
WHERE event_date >= CURDATE() - INTERVAL 30 DAY
GROUP BY place_name
ORDER BY cnt DESC
LIMIT 5;
```

**(라) 많이 쓴 식재료 TOP 5 (30일)**

```sql
SELECT ingredient_name, SUM(total_used_grams) AS g
FROM dashboard_daily_ingredient
WHERE event_date >= CURDATE() - INTERVAL 30 DAY
GROUP BY ingredient_name
ORDER BY g DESC
LIMIT 5;
```

**(마) 많이 남긴 식재료 TOP 5 (30일)** — (라)와 동일 패턴,
`total_leftover_grams` 만 바꿈.

## 7. Spring 어드민 (백오피스 스타일)

참고 디자인: `~/depromeet/18th-team6-server/src/main/resources/templates/admin`
(Django admin 톤 — 네이비 헤더 `#417690`, 노란 브랜드 `#f5dd5d`, 밝은 그레이
배경, `.module results` 카드, 표 중심).

### 7.1 파일 목록

```
src/main/java/com/kirozero/netzero/domain/dashboard/
  ├─ controller/
  │    ├─ DashboardController.java        Thymeleaf 페이지 렌더
  │    └─ DashboardApiController.java     5개 JSON 엔드포인트
  ├─ service/
  │    └─ DashboardQueryService.java      집계 조회 + 윈도 채움
  ├─ repository/
  │    ├─ DashboardDailyMetricRepository.java
  │    ├─ DashboardDailyIngredientRepository.java
  │    └─ DashboardDailyPlaceRepository.java
  ├─ entity/
  │    ├─ DashboardDailyMetric.java
  │    ├─ DashboardDailyIngredient.java
  │    ├─ DashboardDailyPlace.java
  │    └─ BatchJobHistory.java
  ├─ batch/                              (§5)
  └─ dto/
       ├─ ImpactSummaryResponse.java
       ├─ DailyTrendResponse.java
       └─ TopItemResponse.java
src/main/resources/templates/admin/
  └─ dashboard.html
src/main/resources/static/admin/
  └─ admin.css
```

### 7.2 컨트롤러

`DashboardController` — 페이지 셸 + 마지막 갱신 날짜:

```java
@GetMapping("/admin/dashboard")
public String dashboard(Model model) {
    model.addAttribute("lastUpdatedDate", queryService.findLastSuccessDate());
    return "admin/dashboard";
}
```

`DashboardApiController`:

| 메서드 | 경로 | 반환 DTO |
|---|---|---|
| GET | `/admin/api/metrics/impact` | `ImpactSummaryResponse` |
| GET | `/admin/api/metrics/daily-trend` | `DailyTrendResponse` |
| GET | `/admin/api/metrics/top-places` | `List<TopItemResponse>` |
| GET | `/admin/api/metrics/top-used-ingredients` | `List<TopItemResponse>` |
| GET | `/admin/api/metrics/top-leftover-ingredients` | `List<TopItemResponse>` |
| POST | `/admin/api/batch/run?date=YYYY-MM-DD` | 수동 트리거 (데모용) |

응답 예시:

```jsonc
// /admin/api/metrics/impact
{
  "totalFoodProcessedKg": 32.5,
  "totalCarbonSavedKgco2e": 67.8,
  "totalParticipants": 124,
  "totalCompletedSessions": 31,
  "treeEquivalent": 3.08
}

// /admin/api/metrics/daily-trend
{
  "labels": ["2026-06-23","2026-06-24","2026-06-25","2026-06-26","2026-06-27"],
  "series": {
    "sessions":     [2,5,4,6,3],
    "participants": [8,18,14,22,12],
    "foodGrams":    [1600,4200,3800,5400,2900],
    "carbonKg":     [1.8,4.6,4.1,5.7,3.0]
  }
}

// /admin/api/metrics/top-*
{ "items": [ { "name": "양파", "value": 850 }, /* ... */ ] }
```

`treeEquivalent` = `totalCarbonSavedKgco2e / 22` (반올림 둘째 자리).

### 7.3 서비스

`DashboardQueryService` 메서드 5개, 각각 §6.5의 SQL을 호출하고 DTO로
변환. 일별 추이에서 빈 날짜 0 채움은 이 서비스 안에서.

### 7.4 페이지 (`templates/admin/dashboard.html`)

Django-admin 톤 그대로. 구성:

- `.admin-header` — 좌측 "KiroZero administration"(노란 브랜드),
  우측 nav 한 줄 ("Dashboard" 활성).
- `.breadcrumbs` — `Home › Dashboard` + 우측에 "Last updated: YYYY-MM-DD".
- `main.content`:
  1. `content-header` — `<h1>Impact Dashboard</h1>` + 우측 "Last 30 days" 라벨.
  2. `.kpi-grid` (5칸) — KPI 카드 5개 (음식물kg / 탄소kg / 참여자 / 완료 세션 /
     나무 환산).
  3. `.module.results` — "Last 5 days trend" — Chart.js 영역 차트 1개,
     4시리즈 듀얼 Y축(좌: 카운트, 우: kg).
  4. `.module.results` — "Top places (30 days)" — `<table>` 5행.
  5. `.analytics-grid` (2열) — "Top used ingredients" + "Top leftover
     ingredients" 표 2개.

프론트 동작:

- 페이지 로드 시 `Promise.allSettled([impact, dailyTrend, topPlaces,
  topUsed, topLeftover])`.
- 각 fetch 독립 처리: 실패한 카드만 "Failed to load" 표시, 나머지는 정상.
- Chart.js 1개(CDN 한 줄). 자동 새로고침 없음.

### 7.5 admin.css

참고 프로젝트의 admin.css(네이비 헤더, 그레이 배경, `.module results`,
표 스타일)를 그대로 가져온 뒤 KPI/차트용 클래스만 덧붙임:

```css
.kpi-grid { display: grid; grid-template-columns: repeat(5, 1fr); gap: 12px; margin-bottom: 18px; }
.kpi-card { background: #fff; border: 1px solid #e0e0e0; padding: 16px 18px; border-radius: 4px; }
.kpi-label { color: #888; font-size: 12px; text-transform: uppercase; letter-spacing: .05em; }
.kpi-value { color: #333; font-size: 22px; font-weight: 600; margin-top: 4px; }
.kpi-unit  { color: #888; font-size: 12px; margin-left: 2px; }
.chart-card { background: #fff; border: 1px solid #e0e0e0; padding: 16px; }
.chart-card canvas { width: 100%; height: 280px; }
.analytics-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; }
```

### 7.6 라우팅·인증

- `/admin/dashboard`, `/admin/api/metrics/**`, `/admin/api/batch/**`
  현 어드민 정책을 따라 인증 미적용(이번 스코프 밖).
- 정적 자원 `/admin/admin.css` 는 `src/main/resources/static/admin/`
  에서 그대로 서빙.

## 8. 에러 처리 (최소)

| 지점 | 처리 |
|---|---|
| 이벤트 로그 발행 실패 | `EventLogger.emit` 안에서 try/catch + WARN |
| 배치 예외 | 트랜잭션 롤백 + `batch_job_history.status='FAILED'` + `error_message`. 다음날 03:00 또는 수동 트리거로 재실행 |
| 대시보드 API 조회 실패 | `@ExceptionHandler` 하나로 500 반환. 프론트는 `Promise.allSettled` 라 카드별로 빈 칸 |

알림·리트라이 큐·모니터링 안 만듦.

## 9. 운영 메모

- 로컬: `./gradlew bootRun` → `http://localhost:8080/admin/dashboard`.
- 데모용 수동 트리거: `POST /admin/api/batch/run?date=YYYY-MM-DD`.
- 로그 위치: `${LOG_DIR}` 환경변수 (dev `./build/event-logs`,
  prod `/var/log/kiro`).
- 배치/어플이 같은 JVM에서 동작(같은 호스트 파일 시스템 접근).
- 시간대는 전부 `Asia/Seoul`. `date` 필드는 KST 일자.

## 10. 스코프 밖

- 인증/인가, 알림, Prometheus, 다환경 분리(`env` 값만 바꿈).
- 자동 새로고침, 실시간 스트리밍.
- 누적 고유(distinct) 참여자 수.
- 테스트 자동화. 페이지 열어서 차트가 뜨면 끝.
- 배치 실패 알림, 리트라이 큐, 데드레터.
