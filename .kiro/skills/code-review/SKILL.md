---
name: code-review
description: 워크플로 5단계. 작성된 코드/PR/diff 를 머지 전 검토. correctness, JPA(N+1·LazyInit), 트랜잭션, 예외, 테스트 커버리지, 머지 준비도. 동작이 의심되면 직접 띄워(`./gradlew bootRun`) curl 로 검증한 뒤 결론. "PR 리뷰", "diff 봐줘", "이거 머지해도 돼?", "왜 에러나?" 같은 발언에 트리거.
---

# 5. Code Review

워크플로의 다섯 번째 단계. **이미 작성된 코드**를 머지 전에 본다. 정적 리뷰뿐 아니라 "정말 동작하나?" 가 의심되면 직접 띄워서 증거를 모은다. 구현은 안 한다 — 그건 `code-write`.

## 1. 언제 쓰는가

- 완료된 PR/diff 검토
- "머지해도 되나?" 류 질문
- 사용자가 "안 돼", "에러 떠", "이상해", "왜 이래" — 동작 검증·디버깅
- catch-all exception handler, Security filter, 라우팅처럼 코드만 봐서는 알기 어려운 영역

## 2. 핵심 원칙

- **correctness 와 머지 리스크가 스타일보다 먼저.** 깨진 API, 데이터 정합성, 런타임 크래시, 성능, 테스트 누락.
- **추측 금지.** 동작이 의심되면 직접 띄워서 본다.
- **수정 후 다시 검증.** 빌드 성공은 동작 보증이 아니다.

## 3. 정적 리뷰 흐름

1. PR 의도 파악
2. API 동작 변경 식별
3. Controller 책임 검토
4. Service 비즈니스 로직 검토
5. Repository / JPA 사용 검토
6. 트랜잭션 안전성 검토
7. 예외 처리 검토
8. 테스트 검토
9. 머지 결정

## 4. API 계약

- HTTP method, URL, 상태 코드 적절한가
- request/response DTO 안정적인가 (breaking change 면 명시?)
- entity 직접 노출 없는가
- 페이지네이션 필요한가
- 에러 envelope 일관적인가

## 5. Controller

- thin 한가 (비즈니스 로직 없음)
- request DTO validation 있는가
- Repository 직접 호출 없는가
- 일관된 응답 타입

## 6. Service

- use case 가 분명한가
- 비즈니스 로직이 Service 에 있는가
- 메서드 이름이 의도를 드러내는가
- side effect 가 의도된 것인가
- 외부 호출이 안전하게 처리되는가
- 중복 로직 도입되지 않았는가
- 추상화가 이 변경에서 정당화되는가

## 7. JPA / Repository

- N+1 위험 — `findAll` + lazy 접근 패턴 의심
- 페이지네이션 누락 (대량 조회)
- fetch join / `@EntityGraph` 가 필요한 곳
- 양방향 관계 정말 필요한가
- cascade 옵션이 안전한가
- 인덱스가 필요한가
- LazyInitializationException 가능성 (트랜잭션 밖 lazy 접근)

## 8. 트랜잭션

- 다단 쓰기가 `@Transactional` 로 묶였는가
- `@Transactional` 이 Service 레이어에 있는가
- readOnly 표시가 적절한가
- 외부 API 호출이 트랜잭션 안에 있으면 안 됨
- 이벤트 타이밍 안전한가 (`@TransactionalEventListener`?)
- 부분 쓰기 발생 가능성

## 9. Exception

- 예상 실패가 custom exception 으로 분리되었는가
- not-found / validation / conflict 분리
- HTTP 매핑 일관적인가
- cause 보존
- 광범위 catch 가 정당한가 (catch-all `Exception` 은 일단 안티)
- 내부 디테일 노출 없는가

## 10. Test

- 성공 경로 테스트
- validation 실패 테스트
- not-found, conflict 테스트
- 트랜잭션 민감 케이스 테스트
- 커스텀 쿼리 테스트
- 컨트롤러 계약 테스트
- 엣지 케이스 커버

## 11. 동작 의심 시 — 디버깅·검증 절차

**핵심**: 추측 금지 → 직접 실행해서 증거 수집 → 가설 검증 후에만 수정 → 수정 후 다시 검증.

### Step 1. 증상 명확화

"접속 안 됨" = 흰 화면? 에러 페이지? 응답 코드? 리다이렉트 루프? 묻기 전에 코드 1~2분 훑어 후보 좁힌 뒤 묻는다. 빈손 질문 금지.

### Step 2. 관련 코드 + git 히스토리

- 관련 파일 (controller, security config, exception handler, `application-*.yml`)
- `git log --oneline -20`, 최근 커밋 diff — **버그는 보통 마지막에 손댄 곳에**
- 최근 추가된 catch-all, 새로 켠 보안 옵션, 변경된 라우팅이 자주 범인

### Step 3. 직접 띄운다

```bash
./gradlew bootRun
```

백그라운드로 띄우되 `Started ObritApplication` / `APPLICATION FAILED` / `BUILD FAILED` 중 하나가 나올 때까지 polling. 포트 충돌(8080 사용 중)은 사용자에게 끄도록 요청 — 사용자 서버 마음대로 죽이지 않는다.

### Step 4. curl 로 실제 응답 확인

```bash
# 응답 헤더
curl -s -i http://localhost:8080/<path> | head -20

# 리다이렉트 체인
curl -s -o /dev/null -w "Status: %{http_code}\nRedirects: %{num_redirects}\nFinal URL: %{url_effective}\n" \
  -L --max-redirs 5 http://localhost:8080/<path>

# 세션 유지 (form login 등)
JAR=$(mktemp)
curl -s -c "$JAR" -b "$JAR" ...
```

CSRF 토큰 추출은 `grep -oE` 가 multiline 에서 실패 잦음 — python3 정규식이 안정적.

### Step 5. 가설 한 줄로 적고 그 가설만 검증

**"X 가 Y 를 일으킨다, 왜냐하면 Z 이기 때문이다."**

- 검증 전 코드 수정 금지
- 한 번에 한 가설 — 여러 곳 동시 수정 금지
- 빗나가면 새 가설로. "한 번만 더" 금지

### Step 6. 수정 후 반드시 재기동·재검증

- 띄워둔 서버 종료 후 `./gradlew bootRun`
- 같은 curl 시나리오로 응답 다시 확인
- 서버 로그에 새 stack trace / `예상치 못한 오류` / `NoResourceFoundException` 안 찍히는지

### Step 7. 보고는 검증 결과로

- "고쳤습니다" X
- "`/admin` → 302 → `/login` → 200 흐름 확인" O
- "에러 로그 안 떴다" 만으로 성공 판정 금지 — 응답 자체가 200/302 정상인지 직접 확인

### Step 8. 사이드 이슈 분리

디버깅 중 발견한 별개 문제는 현재 PR 밖으로 빼고 별도 이슈로 안내. 한 번에 다 고치면 회귀 위험 ↑.

### 자주 빠지는 함정

| 함정 | 왜 | 대안 |
|---|---|---|
| "에러 로그 없으니 해결" | 200 이 아닐 수도, catch-all 이 삼킬 수도 | 응답 코드/body 직접 확인 |
| 옵션 A·B 왔다갔다 | 가설 검증 없이 코드부터 만지면 어디서 풀린지 모름 | 한 가설씩 검증 |
| 사용자 환경에서만 재현 안 됨 | 캐시된 빌드 가능 | `./gradlew clean bootRun` |
| catch-all `Exception` 핸들러 | Spring 내부 예외까지 500 으로 변환 | 특정 예외 분리 |

### 이 레포 특이사항

- 프로파일 기본 `dev`, H2 in-memory + `data.sql`, `obrit.admin.username/password = admin/admin`
- prod 는 `ADMIN_USERNAME/PASSWORD` env 필수 — 누락 시 부팅 실패 가능
- `GlobalExceptionHandler` 의 catch-all `handleGenericException` 가 정적 리소스 404 도 500 으로 응답 (별도 이슈 후보)
- `AdminSecurityConfig` + `apiSecurityFilterChain` 두 체인 + `@Order` — Security 문제는 `securityMatcher`/`@Order` 부터
- 빌드 검증: `./gradlew harness` (테스트 전체)

## 12. 머지 결정

- **Must fix**: 데이터 손실, 보안 이슈, 런타임 크래시, broken API, 누락된 critical 트랜잭션
- **Should fix**: N+1 위험, 중요 테스트 누락, 모호한 예외 매핑, 중복된 비즈니스 로직
- **Follow up**: 네이밍, 사소한 스타일, 작은 가독성 개선

## 13. 출력 형식

```
# PR Review — <기능 이름>

## Summary
- 의도: ...
- 전체 리스크: 낮음 | 중간 | 높음
- 결정: approve | request changes | comment

## Critical (must fix)
- <위치>: <문제> → <fix 제안>

## Important (should fix)
- <위치>: <문제> → <fix 제안>

## Minor
- <위치>: <코멘트>

## Missing Tests
- <케이스>: <왜 중요한가>

## Good
- <긍정 포인트>

## Verification (수행한 경우)
- 띄움 / 시나리오 / 관찰된 응답
```

## 14. 하지 말 것

- 코드 작성 X — 그건 `code-write`
- 한 PR 에 다 고치라 요구 X — 사이드 이슈는 분리
- 추상화 더 넣으라는 요구를 기본값으로 X
- "에러 안 떴으니 OK" 결론 X — 응답 직접 확인
- 사용자가 띄워둔 서버 임의 종료 X
- flaky 테스트 자동 재시도 X — 정직하게 보고

## 15. 한 줄 요약

**머지 전 마지막 관문. 추측 대신 띄워서 보고, 스타일 대신 correctness/리스크를 본다.**
