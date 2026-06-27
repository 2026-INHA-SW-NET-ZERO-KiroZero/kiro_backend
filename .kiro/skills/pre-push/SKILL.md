---
name: pre-push
description: 워크플로 6단계. push 직전 무거운 검사 두 가지 + PR 본문 작성. (1) 전체 테스트 실제 통과, (2) 코드 변경이 docs/PRD/api-spec.md, docs/PRD/idea.md 에 반영. 통과하면 PR 템플릿(.github/PULL_REQUEST_TEMPLATE.md)을 사람이 쓴 듯한 톤으로 채워준다. "push it", "ready to push?", "PR 본문 써줘", "PR 디스크립션" 같은 발언에 트리거.
---

# 6. Pre-Push

워크플로 마지막 단계. **push 할 모든 커밋**(`origin/<branch>..HEAD`)을 본다. pre-commit 은 가볍게, 이 단계는 무겁게 — 깨진 빌드/문서 누락의 비용이 크기 때문.

## 1. 언제 쓰는가

- 사용자가 "push it" / "ready to push?" — push **전** 실행
- `.githooks/pre-push` 가 Claude 검증을 요청할 때
- 사용자가 "double-check before push" 라고 할 때
- "PR 본문 써줘" / "PR description" — 검사 통과 후 작성

push 할 게 없으면(`git log origin/<branch>..HEAD` 비어 있음) 즉시 종료.

## 2. 검사 1 — 테스트 통과

**왜 중요한가**: `./gradlew harness` 가 `.githooks/pre-push` 에서 돌지만 `--no-verify` 로 우회 가능. 또 CI 에서 silent fail 도 있을 수 있다. Claude 의 역할은 **결과 해석** — exit code 만 믿지 않는다.

절차:

1. `./gradlew test --daemon` (또는 spotless + ArchUnit 포함 전체면 `./gradlew harness`)
2. 끝까지 기다린다. 부분 로그로 성공 판정 금지
3. 최종 요약 읽기:
   - 모두 green → 통과
   - 실패 → 클래스 + 메서드 + 첫 의미 있는 실패 라인 나열, push 차단
   - 컴파일 에러 → 파일/라인 보고, push 차단
4. push 할 커밋에 **새로 추가됐는데 `@Disabled`/`@Ignore`** 인 테스트가 있으면 flag — 거의 항상 실수

판단 규칙: flaky 자동 재시도 X. 실패를 그대로 보고하고 사용자 결정.

## 3. 검사 2 — 코드와 문서 동기화

**왜 중요한가**: `docs/PRD/api-spec.md`, `docs/PRD/idea.md` 가 코드와 어긋나면 문서가 조용히 썩는다.

절차:

1. `git diff origin/<branch>..HEAD` 로 push 될 diff 확인
2. **외부 동작에 영향 주는** 변경 식별:
   - REST 엔드포인트 신규/변경/삭제 (`@RequestMapping`, `@GetMapping` 등)
   - request/response DTO 필드 변경
   - 에러 코드 또는 응답 envelope 모양 변경
   - 도메인 개념 추가/리네이밍이 `docs/PRD/idea.md` 에 언급됨
3. 각 변경에 대해 **같은 push 안에서** 문서가 업데이트됐는지 확인:
   - REST surface → `docs/PRD/api-spec.md`
   - 제품 개념 → `docs/PRD/idea.md`
4. 불일치마다:
   - 코드 변경 위치(파일+라인)와 stale 한 문서 섹션 지목
   - **구체적 문서 수정안 제안** (정확한 추가/변경 라인)
   - 사용자 확인 후에만 적용
5. 이미 같은 push 에서 문서가 업데이트됐으면 그렇게 명시하고 넘어감

판단 규칙: **외부에서 관찰 가능한 행위** 변경만 flag. 내부 리팩토링(private 메서드 이름 변경 등)은 문서 업데이트 불필요.

## 4. 검사 통과 후 — PR 본문 작성

검사 둘 다 통과하고 사용자가 PR 본문을 요청하거나 push 직후 PR 을 열 의도가 보이면, `.github/PULL_REQUEST_TEMPLATE.md` 를 가벼운 톤으로 채워준다.

### 4.1 작성 원칙

이 단계의 핵심은 **출력 톤**.

1. **가볍게, 짧게.** 모든 변경 나열 금지. Summary 1~2줄, ToDo 3~5개, Changes 굵직한 변경 위주.
2. **AI 티 금지.** "본 PR 은 ~을 구현합니다", "~을 통해 ~할 수 있도록 합니다" 같은 보고서/논문 어투 금지. 동료가 슬랙에 적듯이 담백하게.
   - 좋음: "재계산 흐름에서 detached Trip 이 전파되던 문제 수정"
   - 나쁨: "본 변경사항은 재계산 흐름에서 detached 상태의 Trip 엔티티가 전파되는 문제를 해결하기 위한 것입니다"
3. **불릿은 동사구.** "~함", "~수정", "~추가". 마침표 안 찍는다.
4. **추측 금지.** diff/대화에 없는 영향 범위, 성능 수치, "프로덕션 안정성 향상" 같은 마케팅 문구 금지.
5. **템플릿 헤더(이모지 포함)와 frontmatter 그대로 유지.** 한 줄도 바꾸지 않는다.
6. **빈 섹션은 빈 채로.** "특이사항 없음" 같은 빈말 금지.

### 4.2 PR 본문 절차

원본 템플릿:
```
Closes #

## Summary

## Changes

## Etc
```

1. **컨텍스트 수집** — 우선순위:
   - 대화에서 사용자가 한 작업 설명
   - `git log main..HEAD --oneline` — 브랜치 커밋들
   - `git diff main...HEAD --stat` — 변경 파일 규모
   - 필요하면 `git diff main...HEAD` — 실제 변경
   - 브랜치명에 이슈 번호 보이면(`feature/11/...`) `Closes #11`. 확신 없으면 `Closes #` 그대로 두고 사용자에게 확인
2. **Summary** — 1~2줄. 배경 필요하면 한 줄 더. 그 이상 X
3. **Changes** — 굵직한 변경 3~6개. 커밋 그대로 옮기지 말고 의미 단위로 묶기
4. **Etc** — 리뷰어가 알아두면 좋은 것만. 없으면 비워둔다

### 4.3 예시

작업 맥락: 재계산 흐름에서 detached Trip → LazyInitializationException 버그 수정 + 트랜잭션 경계 조정.

```
Closes #11

## Summary

재계산 흐름에서 detached Trip 이 finalizer 로 넘어가던 문제 수정. 트랜잭션 경계도 같이 정리함.

## Changes

- claim 단계에서 Trip 엔티티 대신 ID 만 반환하도록 변경
- processor 에서 readOnly 트랜잭션으로 Trip 재조회 후 ODsay 호출
- finalizer 를 REQUIRES_NEW 로 분리해 부모 트랜잭션과 격리
- ODsay 호출이 트랜잭션 안에 묶이던 문제 해결

## Etc

- bulk update 로 hold 처리하므로 동시성 영향 없음
```

### 4.4 이슈 작성 (옵션)

PR 대신 사전 작업 이슈를 만들 때 — `.github/ISSUE_TEMPLATE/` 의 세 종류 (`-feature.md`, `-fix.md`, `-chore.md`). frontmatter 가 각자 다르므로 **원본 파일을 그대로 복사**한 뒤 본문만 채운다. 어떤 종류인지 모호하면 한 번만 묻는다.

- Summary 1~2줄, ToDo 3~5개, Other information 있을 때만
- 제목은 frontmatter `title:` 접두사(`[FEATURE]`, `[FIX]`, `[CHORE]`) 유지

## 5. 출력 형식

검사 둘 다 통과:

```
✅ pre-push check passed
  - Tests: 142 passed, 0 failed
  - Docs: in sync
```

문제 있으면:

```
❌ Tests failing
  - UserServiceTest.shouldRejectDuplicateEmail
    → expected DuplicateEmailException, got NullPointerException at UserService.kt:47
  - Action: fix and re-run before pushing

⚠️ Docs out of sync
  - Code: added GET /api/v1/users/{id}/profile (UserController.kt:88)
  - Doc:  docs/PRD/api-spec.md has no entry for this endpoint
  - Proposed addition (apply? y/N):
        ### GET /api/v1/users/{id}/profile
        - Returns the public profile of a user.
        - Response: 200 ApiResponse<UserProfileDto>
```

검사 통과 후 PR 본문은 마크다운 그대로 출력. `gh pr create` 를 사용자가 요청하면 HEREDOC 명령 제안하되 실행 전 제목/이슈 번호 확인.

## 6. 하지 말 것

- **시크릿/커밋 메시지 재검사 X** — pre-commit 의 일
- **자동 코드 수정 X** — 실패 테스트 수정은 사용자 결정
- **silent 문서 편집 X** — 항상 diff 보여주고 확인
- **flaky 자동 재시도 X**
- **무관한 문서 손대지 않음** — push 변경과 관련된 것만
- **PR 본문에 커밋 메시지 복붙 X** — 의미 단위로 묶기
- **"~을 통해 ~를 개선합니다" 어투 X**
- **빈 섹션을 "N/A", "없음" 으로 채움 X** — 그냥 비운다
- **템플릿 `<!-- 주석 -->` 지움 X**
- **Closes 이슈 번호 추측 X** — 확신 없으면 비우고 묻는다

## 7. 한 줄 요약

**pre-commit 이 빠른 차선이라면, pre-push 는 느린 차선 — 테스트는 실제로 통과해야 하고, 문서는 실제로 코드와 맞아야 하고, PR 본문은 사람이 쓴 것처럼 가볍게.**
