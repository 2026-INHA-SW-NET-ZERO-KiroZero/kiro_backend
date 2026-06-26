# 데이터 파이프라인 설계 (Loki 기반 어드민 대시보드)

작성일: 2026-06-27
팀: KiroZero / 서비스: 냉장고 반상회 MVP

## 1. 목표

Spring 백엔드에서 발생하는 도메인 이벤트를 JSON 로그로 찍고, Grafana
Alloy가 수집해 Loki에 적재한 뒤, Spring 어드민 페이지가 LogQL로 Loki를
직접 쿼리해 임팩트 대시보드를 렌더한다.

비목표:

- RDS 직접 집계, DynamoDB 집계, 별도 분석 DB
- Grafana 자체 대시보드(필요해지면 디버깅용으로만 임시 사용)
- 인증/인가, 알림, 다환경 분리, 실시간 스트리밍
- 도메인 이벤트(`ApplicationEventPublisher`)/`@TransactionalEventListener`
  기반 발행 — 비즈니스 서비스 메서드 안에서 SLF4J 로거 호출 한 줄로 끝낸다.

## 2. 전체 흐름

```
Spring 비즈니스 서비스
  └─ kiro.events 로거.info(JSON 필드들)   ← 메서드 내 한 줄 추가
        │
        ▼ Logback JSON appender (logstash-logback-encoder)
   컨테이너 stdout
        │
        ▼ docker socket
Grafana Alloy
  └─ loki.source.docker → loki.process(JSON 파싱+라벨링) → loki.write
        │
        ▼ HTTP push
Loki (monolithic, filesystem, 보존 30일)
        │
        ▲ LogQL query / query_range
Spring DashboardApiController
  └─ LokiClient(WebClient) → DTO 가공
        │
        ▼ JSON
어드민 페이지 (/admin/dashboard) — fetch + Chart.js
```

## 3. 컨테이너 구성

기존 `docker-compose.yml`에 두 서비스만 추가한다.

| 서비스 | 이미지 | 포트 |
|---|---|---|
| loki | `grafana/loki:3.0.0` | 3100 |
| alloy | `grafana/alloy:latest` | 12345 (UI) |

Spring 앱 컨테이너와 같은 네트워크에 묶는다. Loki 데이터는 named volume.

## 4. 로그 이벤트 스키마

### 4.1 공통 필드

```json
{
  "ts": "RFC3339Nano",
  "app": "kiro-backend",
  "env": "dev",
  "event": "<이벤트명>"
}
```

Loki 라벨로 승격되는 값: `app`, `env`, `event` 셋뿐.
`slot_id`, `user_id`, `ingredient_name` 등 고카디널리티 값은 라벨로 올리지
않고 JSON 본문에만 둔다.

### 4.2 `session_completed`

발생 시점: `ConsumptionRecord` 저장이 끝난 서비스 메서드의 마지막 줄.

| 필드 | 타입 | 비고 |
|---|---|---|
| slot_id | int | |
| date | string (YYYY-MM-DD) | |
| place_name | string | |
| station_code | string | |
| participant_count | int | |
| menu_name | string | |
| menu_type | enum | `GENERAL`/`LOW_CARBON` |
| finished_food_rate | int | 0–100 |
| total_leftover_input_grams | number | |
| total_leftover_used_grams | number | |
| avg_ingredient_use_rate | int | 0–100 |
| estimated_food_waste_reduced_grams | number | |
| estimated_carbon_saved_kgco2e | number | |

### 4.3 `ingredient_used`

발생 시점: 같은 서비스 메서드 안에서 `ConsumptionRecordItem` 저장 직후,
재료별로 줄 단위 발행.

| 필드 | 타입 |
|---|---|
| slot_id | int |
| date | string |
| ingredient_name | string |
| input_grams | number |
| used_grams | number |
| leftover_grams | number |
| use_rate | int |

### 4.4 `participant_joined`

발생 시점: `SessionParticipant`를 저장한 서비스 메서드의 마지막 줄.

| 필드 | 타입 |
|---|---|
| slot_id | int |
| date | string |
| user_id | int |

## 5. Loki 설정

`infra/loki/local-config.yaml` — monolithic, filesystem 스토리지,
보존 720h(30일), `auth_enabled: false`, schema v13 + tsdb,
`compactor.retention_enabled: true`.

## 6. Alloy 설정

`infra/alloy/config.alloy` 핵심 컴포넌트:

- `discovery.docker` + `discovery.relabel`로 Spring 컨테이너만 매칭
  (컨테이너명 `kiro-app`).
- `loki.source.docker`로 stdout 수집.
- `loki.process`에서:
  - `stage.json`으로 `event`, `app`, `env`, `ts` 추출
  - `stage.labels`로 `event`, `app`, `env`만 라벨 승격
  - `stage.timestamp`로 `ts`를 로그 타임스탬프로 사용
  - `stage.match { selector = "{event=\"\"}", action = "drop" }`로
    이벤트 필드 없는 부팅·일반 로그는 폐기
- `loki.write`로 `http://loki:3100/loki/api/v1/push` 송출.

## 7. Spring 로그 발행

### 7.1 Logback 설정

`src/main/resources/logback-spring.xml`:

- 일반 콘솔 appender(사람이 읽는 포맷)는 그대로.
- `EVENT_JSON` appender 추가 — `net.logstash.logback.encoder.LogstashEncoder`,
  `customFields`로 `app="kiro-backend"`, `env="${APP_ENV:-dev}"` 항상 주입.
- 로거 `kiro.events` 를 INFO + `EVENT_JSON` 전용으로 분리, `additivity=false`.

### 7.2 발행 코드 패턴

비즈니스 서비스 안에서 직접 한 줄:

```java
private static final Logger EVENT = LoggerFactory.getLogger("kiro.events");

EVENT.info("", StructuredArguments.entries(Map.of(
    "event", "session_completed",
    "slot_id", slotId,
    "date", date.toString(),
    "place_name", placeName,
    /* ... 4.2 표의 나머지 필드 ... */
)));
```

`ingredient_used`는 같은 메서드 안에서 항목별 for문으로 발행한다.
`participant_joined`는 참여 저장 메서드에서 한 줄.

발행 호출은 `try` 블록으로 감싸 실패해도 비즈니스 메서드를 깨지 않는다.
DTO, 도메인 이벤트, listener는 만들지 않는다.

## 8. Spring 어드민 측

### 8.1 파일 목록 (이게 전부)

```
docker-compose.yml                            (loki, alloy 서비스 추가)
infra/loki/local-config.yaml                  (신규)
infra/alloy/config.alloy                      (신규)
build.gradle                                  (의존성 2개 추가)
src/main/resources/logback-spring.xml         (신규)
src/main/resources/templates/admin/dashboard.html  (신규)
src/main/java/.../dashboard/LokiClient.java
src/main/java/.../dashboard/DashboardApiController.java
src/main/java/.../dashboard/DashboardController.java
```

기존 서비스 코드 수정 = 7.2 패턴의 로그 호출 한 줄씩 추가뿐.

### 8.2 의존성

`build.gradle`에 추가:

- `net.logstash.logback:logstash-logback-encoder:7.4`
- `org.springframework.boot:spring-boot-starter-webflux` (WebClient용)

Thymeleaf는 이미 쓰고 있으면 추가 불필요.

### 8.3 LokiClient

`WebClient` 한 개를 보유. 메서드 둘:

- `queryInstant(String logql) → LokiInstantResponse`
  - `GET /loki/api/v1/query?query=<logql>`
- `queryRange(String logql, Instant start, Instant end, Duration step)
  → LokiRangeResponse`
  - `GET /loki/api/v1/query_range`
  - `start`, `end`는 epoch 나노초로 변환, `step`은 `<초>s` 문자열.

설정:

```yaml
loki:
  base-url: http://loki:3100
  timeout-seconds: 5
```

타임아웃 초과/HTTP 4xx·5xx → `LokiQueryException`(런타임). 컨트롤러에서
`@ExceptionHandler`로 502 매핑.

### 8.4 DashboardApiController

| 메서드 | 경로 | 반환 |
|---|---|---|
| GET | `/admin/api/metrics/impact` | 누적 카운터 5개 |
| GET | `/admin/api/metrics/daily-trend` | 최근 5일 일별 4시리즈 |
| GET | `/admin/api/metrics/top-places` | 인기 장소 TOP 5 |
| GET | `/admin/api/metrics/top-used-ingredients` | 사용 식재료 TOP 5 |
| GET | `/admin/api/metrics/top-leftover-ingredients` | 남은 식재료 TOP 5 |

각 메서드는 LogQL 문자열을 만들고 `LokiClient` 호출 후 단순 DTO로 가공.
별도 서비스 레이어 없이 컨트롤러 안에서 직접 처리.

응답 예:

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
{ "items": [ { "name": "양파", "value": 850 }, ... ] }
```

`treeEquivalent` = `totalCarbonSavedKgco2e / 22` (반올림 둘째자리). 컨트롤러
계산.

빈 날(이벤트 0건)은 0으로 채워서 5개 라벨이 항상 정렬되도록 한다.

### 8.5 DashboardController

GET `/admin/dashboard` → `templates/admin/dashboard.html` 렌더.
서버는 셸 HTML만 내고 데이터는 위 API들을 페이지 로드 시 fetch.

### 8.6 페이지

Thymeleaf 템플릿 한 파일. Chart.js CDN 한 줄.
세 구역(누적 카운터 / 5일 추이 / TOP 3개)을 fetch 결과로 채운다. 별도
인증·헤더·푸터·스타일은 만들지 않는다.

## 9. LogQL 쿼리 모음

전부 셀렉터 베이스: `{app="kiro-backend", env="dev"}` (env는 설정값으로
치환).

### 9.1 누적 카운터 (30일 윈도)

```
# 음식물(g)
sum(sum_over_time(
  {app="kiro-backend", event="session_completed"}
  | json | unwrap total_leftover_used_grams [30d]))

# 탄소(kgCO2e)
sum(sum_over_time(
  {app="kiro-backend", event="session_completed"}
  | json | unwrap estimated_carbon_saved_kgco2e [30d]))

# 완료 세션 수
count_over_time({app="kiro-backend", event="session_completed"}[30d])

# 참여자 수 (이벤트 누적, 중복 포함)
count_over_time({app="kiro-backend", event="participant_joined"}[30d])
```

음식물(g)은 컨트롤러에서 kg로 환산.

### 9.2 최근 5일 일별 추이 (`query_range`)

`start = today_local - 4d 00:00`, `end = today_local 23:59`, `step = 24h`
(오늘 포함 최근 5일).

```
count_over_time({app="kiro-backend", event="session_completed"}[1d])
count_over_time({app="kiro-backend", event="participant_joined"}[1d])
sum_over_time({app="kiro-backend", event="session_completed"}
  | json | unwrap total_leftover_used_grams [1d])
sum_over_time({app="kiro-backend", event="session_completed"}
  | json | unwrap estimated_carbon_saved_kgco2e [1d])
```

### 9.3 TOP 5 (30일 윈도)

```
# 인기 장소
topk(5, sum by (place_name) (
  count_over_time({app="kiro-backend", event="session_completed"}
    | json place_name="place_name" [30d])))

# 가장 많이 사용된 식재료
topk(5, sum by (ingredient_name) (
  sum_over_time({app="kiro-backend", event="ingredient_used"}
    | json ingredient_name="ingredient_name"
    | unwrap used_grams [30d])))

# 가장 많이 남긴 식재료
topk(5, sum by (ingredient_name) (
  sum_over_time({app="kiro-backend", event="ingredient_used"}
    | json ingredient_name="ingredient_name"
    | unwrap leftover_grams [30d])))
```

`| json <key>="<key>"` 패턴으로 쿼리 시점에만 임시 라벨화하므로
영구 카디널리티 문제는 없다.

## 10. 에러 처리

| 실패 지점 | 동작 |
|---|---|
| Spring 이벤트 로그 발행 실패 | try-catch로 삼킴 + 일반 로그에 WARN |
| Alloy/Loki 다운 | 해당 기간 로그 유실 가능. 비즈니스 영향 없음 |
| 대시보드 API → Loki 쿼리 실패 | 컨트롤러 502, 프론트는 카드별 빈 상태 표시 |
| LogQL 결과 0건 | 0 또는 빈 배열 반환 |

페이지의 5개 fetch는 독립이라 하나 실패해도 나머지 카드는 정상 렌더.

## 11. 운영 메모

- 로컬: `docker compose up -d loki alloy && ./gradlew bootRun`,
  브라우저 `http://localhost:8080/admin/dashboard`.
- Loki 보존 30일, 디스크는 named volume.
- LogQL 디버깅이 필요해지면 `grafana/grafana` 컨테이너를 임시로 띄워
  Loki 데이터소스로 붙여서 사용한 뒤 다시 끈다. 평소 compose에는 두지
  않는다.

## 12. 스코프 밖

- 인증/인가, 세션 관리
- 알림, Prometheus 메트릭 수집
- 다환경(stage/prod) 분리 (`env` 라벨 값만 바꾼다)
- 누적 **고유** 참여자 수(distinct) — Loki에서 안 다룬다. MVP는 이벤트
  누적 카운트만.
- 자동 새로고침, 실시간 스트리밍
- 테스트 자동화. 페이지 열어서 차트가 뜨면 끝.
