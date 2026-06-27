---
name: debug-and-verify-locally
description: 이 레포에서 발생한 버그, 에러 응답, 예상치 못한 동작을 디버깅할 때 사용한다. 사용자가 "안 돼", "에러 떠", "이상해", "왜 이래", "디버깅 해줘", "확인해줘" 같은 말을 하거나, 로컬·운영 환경에서 특정 엔드포인트나 페이지가 동작하지 않는다고 보고할 때 반드시 트리거. 추측 대신 직접 ./gradlew bootRun 으로 서버를 띄우고 curl/로그로 증거를 수집해서 가설을 검증한 뒤 수정하고, 수정 후에도 다시 띄워서 동작을 확인하는 작업 방식을 강제한다. Spring Security, 라우팅, 시큐리티 필터, 인증 흐름, 예외 핸들러 같이 "코드만 봐서는 동작을 알기 어려운" 영역일수록 우선 적용.
---

# Debug and Verify Locally

이 레포의 버그를 추적할 때 따르는 작업 방식. 핵심은 **추측 금지 → 직접 실행해서 증거 수집 → 가설 검증 후에만 수정 → 수정 후 다시 검증**이다.

## 1. 언제 쓰는가

- 사용자가 특정 페이지·엔드포인트가 안 된다고 보고할 때 (`/admin`, `/login`, API 등)
- 에러 응답을 들고 와서 원인을 묻거나 고쳐달라고 할 때 (`status:500`, `999`, `403`, `ERR_TOO_MANY_REDIRECTS` 등)
- 동작이 코드와 다르게 보이는 모든 경우 (시큐리티 필터, 예외 핸들러, 트랜잭션 등 우회로가 많은 영역)
- "왜 이래" / "이상하다" 류 모호한 보고

## 2. 작업 순서

다음 순서를 깨면 거의 항상 잘못된 곳을 고치게 된다.

### Step 1. 증상을 사용자 말로 한 번 더 명확히 잡는다

"접속이 안 된다"는 말이 의미하는 것:
- 흰 화면? 에러 페이지? 응답 코드? 리다이렉트 루프?
- 어떤 입력에서? 어떤 환경에서?

추측이 갈리는 단어가 있으면 사용자에게 한 줄로 물어본다. 단, 사용자에게 묻기 전에 코드 1~2분 훑어서 후보를 좁힌 다음 묻는다. 빈손으로 묻지 않는다.

### Step 2. 관련 코드와 git 히스토리를 먼저 본다

- 관련 파일을 읽는다 (controller, security config, exception handler, application-*.yml)
- `git log --oneline -20` 그리고 해당 영역에 최근 손댄 커밋의 diff를 본다 — **버그는 보통 마지막에 손댄 곳에 있다**
- 최근에 추가된 catch-all, 새로 켠 보안 옵션, 변경된 라우팅이 자주 범인이다

### Step 3. 직접 서버를 띄워 증거를 모은다

코드만 보고 결론 내지 않는다. 직접 띄운다.

```bash
./gradlew bootRun
```

백그라운드로 띄우되 부팅 완료를 명확히 기다린다 (`Started ObritApplication` / `APPLICATION FAILED` / `BUILD FAILED` 셋 중 하나가 나올 때까지 polling).

포트 충돌(8080 already in use)이 나면 사용자에게 끄도록 요청한다. 사용자가 띄워둔 서버를 마음대로 죽이지 않는다.

### Step 4. curl로 요청을 흘려보고 실제 응답을 본다

브라우저 증상은 사용자 화면에서, 응답·헤더·리다이렉트 체인은 curl 로 본다.

```bash
# 단일 요청 응답 헤더
curl -s -i http://localhost:8080/<path> | head -20

# 리다이렉트 체인 전체
curl -s -o /dev/null -w "Status: %{http_code}\nRedirects: %{num_redirects}\nFinal URL: %{url_effective}\n" \
  -L --max-redirs 5 http://localhost:8080/<path>

# 세션 유지가 필요한 경우 (form login 등) cookie jar 사용
JAR=$(mktemp)
curl -s -c "$JAR" -b "$JAR" ...
```

CSRF 토큰처럼 HTML 폼에서 추출해야 하는 값은 `grep -oE` 가 multiline 에서 자주 실패한다. python3 정규식으로 뽑는 게 안정적이다.

### Step 5. 가설을 한 줄로 적고 그 가설만 검증한다

좋은 가설은 이렇게 생겼다: **"X가 Y를 일으킨다, 왜냐하면 Z이기 때문이다."**

- 가설 검증 전에는 코드 안 고친다
- 한 번에 한 가설씩 — 여러 곳을 동시에 고치면 무엇이 효과 있었는지 모른다
- 빗나가면 새 가설로 돌아간다. "한 번만 더 시도" 금지

### Step 6. 수정 후 반드시 다시 띄워서 검증한다

빌드가 통과한다는 것은 "동작한다"는 뜻이 아니다.

- 코드 수정
- 띄워둔 서버 종료 후 재기동 (`./gradlew bootRun`)
- 같은 curl 시나리오로 다시 응답 확인
- **"이전 검증 단계와 동일하거나 더 좋아졌는가"** 가 통과 기준

서버 로그도 같이 확인: `예상치 못한 오류`, `NoResourceFoundException`, stack trace 등이 새로 찍히지 않는지.

### Step 7. 보고는 검증 결과로 한다

- "고쳤습니다"가 아니라 **"`/admin` → 302 → `/login` → 200 까지 흐름 확인"** 같이 검증한 사실을 말한다
- 사용자가 "에러 로그 안 떴다"고 해도 그게 "성공"인지 확인하기 전에 결론 내지 않는다 — 응답 자체가 200/302 정상 상태인지 직접 본다
- 사용자 환경에서 한 번 더 확인을 요청할 때는 무엇을 어떻게 확인해야 하는지 명시한다

### Step 8. 사이드 이슈는 분리한다

디버깅 중에 발견한 별개 문제(예: catch-all 예외 핸들러의 부작용)는 **현재 PR 범위 밖**으로 빼고 별도 이슈로 올리도록 안내한다. 한 번에 다 고치면 리뷰가 어렵고 회귀 위험이 커진다.

## 3. 자주 빠지는 함정

| 함정 | 왜 안 좋은가 | 대안 |
|---|---|---|
| "에러 로그가 안 떴으니 해결" | 200 OK 가 아닐 수도 있다 (catch-all 핸들러가 삼킴, 리다이렉트만 됐을 수 있음) | 응답 코드와 body 까지 직접 확인 |
| 옵션 A·B 왔다갔다 | 가설 검증 없이 코드부터 만지면 어디서 풀렸는지 모름 | 한 가설씩 검증, 결정적 증거 확보 후 적용 |
| 사용자 환경에서만 재현 안 됨 | 캐시된 빌드, 옛 코드일 수 있음 | `./gradlew clean bootRun` 한 번 |
| "옜날에 이 옵션 넣어야 한다고 해서" | 과거 결정이 지금 충돌의 원인일 수 있음 | `git log -- <file>` 로 도입 맥락 확인 후 판단 |
| catch-all `Exception` 핸들러 | Spring 내부 예외까지 500으로 변환되어 진짜 원인을 가린다 | 특정 예외는 명시적으로 분리 처리 |

## 4. 이 레포 특이사항

- 프로파일 기본은 `dev`, H2 in-memory + `data.sql`, `obrit.admin.username/password` = `admin/admin`
- prod 는 `ADMIN_USERNAME/PASSWORD` env 필수 — env 누락 시 빈 자격증명 / 부팅 실패 가능성
- `GlobalExceptionHandler` 에 catch-all `handleGenericException` 이 있어 정적 리소스 404 같은 Spring 내부 예외도 500으로 응답한다 (별도 이슈 대상)
- `AdminSecurityConfig` 와 `apiSecurityFilterChain` 두 체인 + `@Order` — 시큐리티 문제는 `securityMatcher` 와 `@Order` 부터 본다
- 빌드 검증: `./gradlew harness` (ktlint + ArchUnit + 테스트)

## 5. 한 줄 요약

**띄우지 않고 고친 건 고친 게 아니다.**
