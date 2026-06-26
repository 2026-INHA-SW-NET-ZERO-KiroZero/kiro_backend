---
name: organize-domain-model
description: Use when writing Spring Boot Kotlin code in this repo — a short checklist of code review lessons. Currently covers (1) where domain enum/value class lives, (2) who decides domain state, (3) which domain owns a read-model. Triggers when adding a new enum/value class, when an enum sits inside a DTO file, when state-deciding logic is piling up in a Service, or when a new *Snapshot/*Projection/*View class is being placed.
---

# Organize Domain Model

코드 리뷰에서 반복되는 지적을 짧게 모아둔 체크리스트. 새 항목은 아래에 한 단락씩 추가한다.

## 1. 도메인 enum/value class는 `entity/`에 둔다

- DTO 파일 안에 도메인 enum을 정의하지 않는다. DTO는 import해서 쓴다
- `enums/`, `types/` 같은 타입 기준 디렉토리를 만들지 않는다
- 연관된 도메인(`item/entity/`, `home/entity/` 등)에 둔다 — `Item`을 읽을 때 `ItemStatus`가 옆에 있도록

```kotlin
// ❌ home/dto/HomeDto.kt
enum class ItemStatus { GOOD, WARNING, DANGER }

// ✅ item/entity/ItemStatus.kt
package depromeet.hotsix.obrit.item.entity
enum class ItemStatus { GOOD, WARNING, DANGER }
```

## 2. 단순한 상태 판단은 도메인 객체가 한다 (Tell, Don't Ask)

- 한 도메인의 필드만으로 결정되는 단순한 판단은 도메인 메서드로 둔다. Service가 필드를 꺼내 `if`/`when`으로 분기하고 있다면 옮길 후보
- 단, **Service = 오케스트레이션만**은 아니다. 여러 도메인을 조합하거나 정책/가중치/외부 데이터가 섞이는 복잡한 판단은 Service에 둔다
- 기준: "이 판단이 도메인 객체 자신만 알면 되는 일인가?" → 예: 도메인으로, 아니오: Service로
- `entity`는 다른 레이어를 의존하지 않는다 — Repository는 직접 호출하지 말고 필요한 값은 인자로 받기

```kotlin
// ❌ Service가 직접 분기
private fun spareBand(item: ItemSnapshot) =
    if (item.quantity == 0) SpareBand.NONE else SpareBand.HAS

// ✅ 도메인이 자기 상태를 안다
data class ItemSnapshot(/* ... */) {
    fun spareBand() = if (quantity == 0) SpareBand.NONE else SpareBand.HAS
}
```

## 3. Read-model(`*Snapshot`/`*Projection`/`*View`)은 데이터 출처 도메인에 둔다

- 데이터가 어디서 오는지로 소유 도메인을 정한다. 누가 소비하는지로 정하지 않는다
- 단일 객체에 대한 판단 메서드(`band()`, `score()` 등)는 read-model에 함께 둬도 된다 — 그 객체 자신만의 일이라면 (규칙 2와 동일)
- 여러 객체를 모은 집계/요약/버킷화는 소비 도메인의 Service가 한다
- 이름이 소비 도메인 용어(예: home 화면용인데 `HomeItemSnapshot`)이면 위치를 잘못 잡았는지 의심

```kotlin
// ❌ home이 item 데이터를 자기 entity로 정의 — item.service가 home.entity를 만들어 순환 발생
// home/entity/ItemSnapshot.kt
data class ItemSnapshot(val id: Long, val quantity: Int, ...) { ... }

// ✅ 데이터 출처(item)가 소유, home은 입력으로 받아 집계만
// item/entity/ItemSnapshot.kt
data class ItemSnapshot(val id: Long, val quantity: Int, ...) {
    fun spareBand(): SpareBand = ...   // 단일 item 판단은 OK
}
// home/service/HomeStatusCalculatorService.kt
fun calculate(items: List<ItemSnapshot>): OverallStatusResponse { ... }   // 집계는 여기
```

> 다른 도메인의 read-model을 Service가 참조해야 한다. `ArchitectureRules.isShareableDomainType`이 `*Snapshot`/`*Projection`/`*View` 접미사를 공유 허용 타입으로 인정하므로 위반이 아니다. 일반 entity는 여전히 차단된다.

---

> 새 지침을 추가할 때: 한 섹션은 **규칙 한 줄 + 이유 한 줄 + Before/After 한 쌍**으로 짧게.
