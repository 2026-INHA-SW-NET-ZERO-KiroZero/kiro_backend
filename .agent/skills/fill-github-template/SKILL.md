---
name: fill-github-template
description: when2go 저장소의 PR/이슈 템플릿(.github/PULL_REQUEST_TEMPLATE.md, .github/ISSUE_TEMPLATE/*.md)을 자동으로 채워주는 스킬. 사용자가 "PR 본문 써줘", "이슈 만들어줘", "PR 템플릿 채워줘", "feature 이슈 작성", "PR 디스크립션", "PR description" 등을 말하거나, 작업을 마치고 PR/이슈를 열려고 할 때 사용. 사람이 직접 쓴 듯한 담백한 톤으로, 핵심만 가볍게 정리해서 템플릿 형식에 맞춰 출력한다.
---

# Fill GitHub Template

when2go 프로젝트의 PR/이슈 템플릿을 작성한다. 목표는 **사람이 직접 쓴 듯한 가벼운 본문**을 템플릿 형식 그대로 채워서 내놓는 것.

## 언제 쓰는가

- 작업 마치고 PR 본문 정리할 때
- 새 작업 시작 전 이슈 등록할 때
- 사용자가 "PR 템플릿 채워줘", "이슈 작성해줘", "PR description 써줘" 같이 말할 때

## 작성 원칙

이 스킬의 핵심은 **출력 톤**이다. 다음을 지킨다.

1. **가볍게, 짧게.** 모든 변경/할 일을 다 나열하지 말고 핵심만 추린다. Summary는 1~2줄, ToDo는 3~5개, Changes는 굵직한 변경 위주.
2. **AI 티 내지 않는다.** "본 PR은 ~을 구현합니다", "~을 통해 ~할 수 있도록 합니다" 같은 보고서/논문 어투 금지. 동료가 슬랙에 적듯이 담백하게.
   - 좋음: "재계산 흐름에서 detached Trip이 전파되던 문제 수정"
   - 나쁨: "본 변경사항은 재계산 흐름에서 detached 상태의 Trip 엔티티가 전파되는 문제를 해결하기 위한 것입니다"
3. **불릿은 동사구로 끝나게.** "~함", "~수정", "~추가" 식의 짧은 어미. 마침표는 안 찍는다.
4. **추측은 적지 않는다.** diff/대화에 없는 영향 범위, 성능 수치, "프로덕션 안정성 향상" 같은 마케팅 문구는 빼라.
5. **템플릿의 헤더(이모지 포함)와 frontmatter는 그대로 유지.** 한 줄도 바꾸지 않는다.
6. **빈 섹션은 빈 채로 둔다.** 정보가 없으면 억지로 채우지 말고 헤더만 남긴다. "특이사항 없음" 같은 빈말도 적지 않는다.

## PR 본문 작성 절차

원본 템플릿: `.github/PULL_REQUEST_TEMPLATE.md`

```
Closes #

## Summary

## Changes

## Etc
```

1. **컨텍스트 수집** — 다음을 우선순위대로 확인:
   - 현재 대화에서 사용자가 한 작업 설명
   - `git log main..HEAD --oneline`로 이 브랜치의 커밋들
   - `git diff main...HEAD --stat`로 변경 파일 규모
   - 필요하면 `git diff main...HEAD`로 실제 변경 확인
   - 브랜치명에 이슈 번호가 보이면 (`feature/11/...` 등) 그 번호를 Closes에 쓴다. 확신 없으면 `Closes #` 그대로 두고 사용자에게 확인.

2. **Summary** — 이 PR이 뭘 하는지 1~2줄. 배경이 필요하면 한 줄 더. 그 이상은 쓰지 않는다.

3. **Changes** — 굵직한 변경 3~6개. 커밋 하나하나를 그대로 옮기지 말고, 의미 단위로 묶는다. 같은 영역의 자잘한 수정은 한 줄로 합쳐도 된다.

4. **Etc** — 리뷰어가 알아두면 좋은 것만. 예: "외부 API 호출이 트랜잭션 밖으로 빠짐", "스케줄러 주기 5초". 없으면 비워둔다.

### PR 예시

작업 맥락: 재계산 흐름에서 detached Trip 엔티티가 finalizer까지 흘러가서 LazyInitializationException이 나던 버그를 수정. 트랜잭션 경계도 같이 조정.

```
Closes #11

## Summary

재계산 흐름에서 detached Trip이 finalizer로 넘어가던 문제 수정. 트랜잭션 경계도 같이 정리함.

## Changes

- claim 단계에서 Trip 엔티티 대신 ID만 반환하도록 변경
- processor에서 readOnly 트랜잭션으로 Trip 재조회 후 ODsay 호출
- finalizer를 REQUIRES_NEW로 분리해 부모 트랜잭션과 격리
- ODsay 호출이 트랜잭션 안에 묶이던 문제 해결

## Etc

- bulk update로 hold 처리하므로 동시성 영향 없음
```

## 이슈 본문 작성 절차

세 종류가 있고 frontmatter가 각자 다르므로 **반드시 원본 파일을 읽고 그대로 복사**한 뒤 본문만 채운다.

- `.github/ISSUE_TEMPLATE/-feature.md` — ✨ FEATURE
- `.github/ISSUE_TEMPLATE/-fix.md` — 🐛 FIX
- `.github/ISSUE_TEMPLATE/-chore.md` — 🔧 CHORE

사용자 요청이 어느 쪽인지 모호하면 한 번만 묻는다. ("새 기능이면 feature, 버그면 fix, 그 외 정리/설정이면 chore인데 어느 쪽인가요?")

### 작성 가이드

- **Summary**: 1~2줄. 뭘 만들/고칠 건지.
- **Bug Description** (fix 한정): 어떤 상황에서 무엇이 잘못되는지 2~4줄. 재현 경로가 있으면 같이.
- **ToDo**: 3~5개. 너무 잘게 쪼개지 않는다. "테스트 작성" 같은 항목은 실제로 할 일이면 넣고, 의례적이면 뺀다.
- **Other information**: 참고 링크, 관련 PR/이슈가 있으면. 없으면 비운다.

제목은 frontmatter의 `title:` 접두사(`[FEATURE]`, `[FIX]`, `[CHORE]`)를 유지하고 뒤에 핵심을 한 줄로 붙인다. 예: `title: "[FIX] 재계산 finalizer LazyInitializationException"`

### 이슈 예시 (fix)

작업 맥락: 재계산 스케줄러가 돌 때 ODsay API 호출이 길어지면서 같은 트랜잭션 안에서 DB 락을 오래 잡고 있더라.

```
---
name: "🐛 FIX"
about: 버그 수정 사항을 입력해주세요
title: "[FIX] 재계산 중 ODsay 호출이 DB 트랜잭션에 묶임"
labels: 🐛fix
assignees: 
---

## ✏️ Summary
<!-- 어떤 버그를 수정할 것인지 간략하게 적어주세요 -->
재계산 처리 중 ODsay 외부 API 호출이 DB 트랜잭션 내부에서 일어나 락 점유 시간이 길어짐

## 🚨 Bug Description
<!-- 발생한 버그에 대해 상세히 적어주세요 -->
TripRecalcProcessor가 한 트랜잭션 안에서 Trip 조회 + ODsay 호출 + 업데이트를 다 처리하고 있음. ODsay 응답이 1~2초 걸리는 동안 해당 row가 락에 잡혀 있어 동시 재계산이 막힌다.

## 📝 ToDo
<!-- 수정해야 하는 작업을 적어주세요 -->
- [ ] ODsay 호출을 트랜잭션 밖으로 분리
- [ ] finalizer를 REQUIRES_NEW로 격리
- [ ] 동시 처리 시나리오 테스트 추가

## 📚 Other information
<!-- 참고할 사항이 있다면 적어주세요 -->
```

## 출력 방식

기본적으로 **마크다운을 그대로 본문에 출력**한다. 사용자가 `gh pr create`나 `gh issue create`로 바로 만들어 달라고 하면 HEREDOC으로 명령을 만들어 제안하되, 실행 전 사용자 확인을 받는다 (제목/번호 검증 필요).

## 자주 하는 실수

- ❌ 커밋 메시지를 그대로 ToDo/Changes에 복붙 — 의미 단위로 묶어라
- ❌ Summary에 모든 변경을 다 넣음 — Summary는 "왜/뭘"만, "어떻게"는 Changes
- ❌ "~을 통해 ~를 개선합니다" 같은 보고서 어투
- ❌ 빈 섹션을 "N/A", "없음"으로 채움 — 그냥 비워라
- ❌ 템플릿의 `<!-- 주석 -->`을 지움 — 유지한다
- ❌ Closes 이슈 번호 추측 — 확신 없으면 비워두고 물어본다
