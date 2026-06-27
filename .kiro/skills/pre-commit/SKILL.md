---
name: pre-commit
description: 워크플로 1단계. 커밋 직전 staged diff(`git diff --cached`)에 대해 lint(spotless)가 잡지 못하는 두 가지만 가볍게 확인 — (1) 시크릿/민감정보 누출, (2) 커밋 메시지 품질. "commit this", "is this safe to commit?", "run pre-commit", pre-commit 훅이 Claude 검증을 요청할 때 트리거.
---

# 1. Pre-Commit

워크플로의 첫 단계이자 가장 가벼운 단계. **staged diff 만** 본다. lint 가 잡지 못하는 두 가지를 빠르게 검사한다.

## 1. 언제 쓰는가

- 사용자가 "commit this" / "is this safe to commit?" — 커밋 **전** 실행
- `.githooks/pre-commit` 이 트리거되고 Claude 검증을 요청할 때
- 사용자가 "staged 한 거 가볍게 한 번 봐달라" 고 할 때

`git diff --cached --quiet` 가 true 면 즉시 종료.

## 2. 두 가지 검사

### 검사 1 — 시크릿/민감정보 누출

**왜 중요한가**: 정규식 기반 도구(git-secrets, trufflehog)는 고정된 모양의 토큰만 잡는다. **변수명이나 컨텍스트로만 알 수 있는** 시크릿은 빠져나간다.

절차:

1. `git diff --cached` 를 읽는다.
2. 다음이 보이면 커밋을 막고 보고한다:
   - **이름 있는 시크릿에 실제 값**: `apiKey = "..."`, `password = "..."`, `secret = "..."`, `token = "..."`. `"your-key-here"`, `"changeme"`, `"xxx"` 같은 placeholder 는 OK
   - **환경 설정의 실제 값**: `application-prod.yml`, `application-staging.yml` 등에 실 DB URL/패스워드/키
   - **`.env` 류 파일 staging**: `.env`, `.env.prod`, `.env.local` — 거의 항상 실수
   - **JWT/토큰 모양**: `eyJ...` 로 시작하는 긴 base64, `sk-...`, `ghp_...`, `AKIA...` 같은 prefix
   - **개인정보**: 실제 같은 이메일/전화번호/주민번호 (명확한 테스트 픽스처는 OK)
3. 정확한 파일과 라인을 지목한 뒤 사용자가 직접 unstage 하게 한다. **자동 unstage 금지.**

판단 규칙: 의심스러우면 사용자에게 묻는다. 오탐의 비용은 몇 초, 누출의 비용은 그것보다 훨씬 크다.

### 검사 2 — 커밋 메시지 품질

**왜 중요한가**: 이 레포는 conventional prefix(`chore:`, `feat:`, `fix:`, `ci:` ...)를 쓴다. 메시지가 실제 변경과 안 맞으면 `git log` 가 쓸모 없어진다.

절차:

1. 작성될 커밋 메시지를 가져온다.
2. `git log --oneline -10` 으로 현재 쓰이는 prefix 스타일을 확인하고 똑같이 맞춘다.
3. 메시지와 staged diff 를 비교:
   - **잘못된 prefix**: 실제로는 새 기능(`feat:`)인데 `chore:` 같은 경우
   - **너무 모호함**: `update`, `fix`, `wip` 같은 한 단어
   - **diff 와 불일치**: 메시지는 "swagger setup" 인데 diff 는 controller 까지 건드림
4. 문제 있으면 **막지 말고** 더 나은 메시지 1~2개 제안 후 사용자가 선택.

판단 규칙: 명백히 OK 면 그냥 통과. 잔소리 금지.

## 3. 출력 형식

둘 다 통과면 한 줄:

```
✅ pre-commit check passed (no secrets, commit message OK)
```

문제 있으면 검사당 1~3줄:

```
❌ Possible secret leak
  - application-prod.yml:12 looks like a real DB password ("kiro_2026!")
  - Action: move it to an env var, unstage, then recommit

⚠️ Commit message suggestion
  - Current: "chore: response handling"
  - Suggested: "feat(global): add ApiResponse common wrapper"
```

## 4. 하지 말 것

- **테스트 금지** — pre-push 단계의 일
- **재포맷팅 금지** — spotless 가 이미 함
- **자동 수정 금지** — unstage/메시지 재작성은 사용자 결정
- **스타일 잔소리 금지** — naming, 함수 길이 등은 `code-review` 의 일

## 5. 한 줄 요약

**Lint 는 모양을 본다. 이 단계는 컨텍스트만 본다 — 시크릿과 메시지, 두 개만.**
