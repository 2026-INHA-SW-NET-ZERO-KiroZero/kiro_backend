---
name: spec-write
description: 워크플로 2단계. 새 기능/변경을 구현하기 전에 스펙(API 계약, 도메인 모델 위치, 트랜잭션/예외 경계, 적용할 패턴)을 결정해서 한 장짜리 문서로 출력. "스펙 써줘", "설계 먼저", "기능 만들기 전에 정리", "이거 어떻게 설계할까?" 같은 발언에 트리거. 코드 작성 전 단계.
---

# 2. Spec Write

워크플로의 두 번째 단계. **코드를 쓰기 전에** 무엇을 어디에 어떻게 만들지 한 장으로 정리한다. 이 단계의 산출물은 다음 단계(`spec-review`)에서 검토되고, 통과해야 `code-write` 로 넘어간다.

## 1. 언제 쓰는가

- 새 API/기능을 시작할 때
- 기존 도메인에 새 enum/value class/read-model 을 추가할 때
- "이거 어디에 둘까?", "이거 패턴 적용해야 할까?" 류 질문
- 사용자가 "스펙 먼저 정리하자" / "설계부터 가자" 라고 말할 때

## 2. 스펙에 포함해야 할 8개 항목

1. **목표** — 한 문단으로 무엇을, 왜
2. **API 계약** — HTTP method, URL, request/response DTO 모양, 상태 코드, 에러
3. **도메인 모델 변경** — 신규/변경되는 entity, enum, value class, read-model
4. **위치 결정** — 각 신규 타입의 패키지 (규칙은 §4 참고)
5. **상태 판단 위치** — 단순 상태 판단은 도메인, 복잡한 조합은 Service (§5 참고)
6. **트랜잭션/예외 경계** — `@Transactional` 범위, 외부 호출 분리, 예외 종류
7. **패턴 적용 여부** — Builder/Factory/Strategy/Event/Decorator/Adapter 중 무엇을 왜 (§6 참고)
8. **테스트 전략** — 어느 레이어를 어느 방식으로 (§7 참고)

## 3. 핵심 원칙

**가장 단순한 동작 가능 설계부터 시작한다.** Factory/Strategy/Event/Decorator/Adapter 는 현재 요구가 명확히 필요로 할 때만. 추측 기반 추상화 금지.

## 4. 도메인 타입 위치 결정

- 도메인 enum/value class 는 **`{도메인}/entity/`** 에 둔다. DTO 파일 안에 정의 금지
- `enums/`, `types/` 같은 타입 기준 디렉토리 만들지 않는다 — 연관 도메인(`item/entity/`, `home/entity/`) 옆에 둔다
- Read-model(`*Snapshot`, `*Projection`, `*View`)은 **데이터 출처 도메인**이 소유한다 (소비 도메인 X)
  - 예: home 화면이 item 데이터를 모아 보여주면 `ItemSnapshot` 은 `item/entity/`, 집계는 `home/service/`
  - 이름이 소비 도메인 용어(`HomeItemSnapshot` 같은)이면 위치 잘못 잡았는지 의심

```kotlin
// ❌ home/dto/HomeDto.kt 안에 enum 정의
// ✅ item/entity/ItemStatus.kt 에 정의 후 DTO 가 import
```

## 5. 상태 판단 위치

**Tell, Don't Ask.** 한 도메인의 필드만으로 결정되는 단순한 판단은 도메인 메서드로.

- 도메인으로 갈 후보: Service 가 도메인 필드를 꺼내 `if`/`when` 으로 분기하고 있음
- Service 에 남길 후보: 여러 도메인 조합, 정책/가중치/외부 데이터가 섞임
- 기준 질문: "이 판단이 도메인 객체 자신만 알면 되는가?" 예 → 도메인, 아니오 → Service

```kotlin
// ❌ Service 가 분기
if (item.quantity == 0) SpareBand.NONE else SpareBand.HAS

// ✅ 도메인이 자기 상태 판단
data class ItemSnapshot(...) {
    fun spareBand() = if (quantity == 0) SpareBand.NONE else SpareBand.HAS
}
```

## 6. 패턴 적용 여부

**먼저 묻는다**: 단순한 `new`, 메서드 추출, `if` 로 충분한가? → YES 면 패턴 적용 안 함.

| 상황 | 패턴 |
|---|---|
| 파라미터 많고 필수/선택 섞임 | Builder |
| 타입에 따라 생성 객체가 달라짐 (스위치/if-else 가 여러 곳에 흩어짐) | Factory |
| 같은 목적의 알고리즘/정책을 런타임에 교체 | Strategy |
| 상태 변경 후 여러 후속 작업이 필요 (재고 차감, 메일, 쿠폰 등) | Spring Event |
| 기존 객체에 행위 추가 (logging, caching, validation) | Decorator (또는 AOP/Interceptor 가 더 단순할지 먼저 확인) |
| 외부/레거시 인터페이스를 내부 인터페이스로 변환 | Adapter |

핵심: **Factory 는 "무엇을 만들지", Strategy 는 "어떤 행위를 쓸지".** 함께 쓸 수 있다 (Factory 가 Strategy 구현 선택).

이벤트 사용 시 주의: 항상 성공해야 하는 핵심 로직을 이벤트 안에 숨기지 않는다. 트랜잭션 이후 실행이 필요하면 `@TransactionalEventListener` 고려.

## 7. 테스트 전략 결정

- **Unit**: entity, value object, converter, validator, pure service logic
- **Slice**: controller 계약, repository 쿼리만 검증할 때
- **Integration**: Spring wiring, 트랜잭션, JPA 매핑, DB 동작이 리스크에 포함될 때

행위를 증명하는 **최소 범위**를 선택한다.

추가 대상: 도메인 규칙/불변식, 버그 수정(실패 테스트 먼저), converter/parser/calculator, 컨트롤러 계약, 커스텀 쿼리, 트랜잭션 민감 로직, 보안 규칙.

스킵 가능: 단순 필드 추가, trivial getter/setter, 프레임워크 wiring(기존 통합 테스트가 커버하면).

## 8. 출력 형식

```
# Spec — <기능 이름>

## 1. 목표
<한 문단>

## 2. API 계약
- METHOD /path
- Request: <DTO 필드>
- Response: <DTO 필드>, 상태 코드
- 에러: <비즈니스 예외 → HTTP 매핑>

## 3. 도메인 모델 변경
- 신규: <타입> — <필드 요약>
- 변경: <타입> — <변경 요약>

## 4. 위치 결정
- <타입> → `<패키지 경로>` (이유: ...)

## 5. 상태 판단 위치
- <판단> → 도메인 메서드 `<entity>.<method>()`
- <판단> → Service `<svc>.<method>()` (이유: ...)

## 6. 트랜잭션/예외 경계
- `<Service>.<method>` → `@Transactional`
- 외부 API 호출은 트랜잭션 밖
- 예외: <BizException> → <HTTP>

## 7. 패턴 적용
- 적용: <패턴> @ <위치> — <이유>
- 미적용: <패턴> — <단순한 대안으로 충분>

## 8. 테스트 전략
- <레이어/케이스> → <unit | slice | integration>
```

## 9. 하지 말 것

- 코드를 쓰지 않는다 (다음 단계의 일)
- 추측 기반 확장점 추가 ("나중에 필요할 수도 있으니" 금지)
- 위치 결정 없이 "service 에 두면 됩니다" 같은 모호한 답
- 패턴을 적용하고 싶다는 이유로 적용

## 10. 한 줄 요약

**스펙은 결정을 글로 남기는 단계. 위치, 경계, 패턴 적용 여부까지 정리되어야 다음으로 넘어간다.**
