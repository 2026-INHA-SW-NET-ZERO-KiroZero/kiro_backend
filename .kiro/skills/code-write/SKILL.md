---
name: code-write
description: 워크플로 4단계. 통과된 스펙을 받아 Spring Boot 백엔드 코드를 작성. Controller/DTO/Service/Repository/JPA/Exception/Test 전 레이어. JPA N+1·LazyInitializationException 방지, 트랜잭션 경계, 테스트 작성까지 포함. "구현해줘", "코드 작성", "기능 만들어줘" 같은 발언에 트리거. 리뷰/디버깅 X.
---

# 4. Code Write

워크플로의 네 번째 단계. **스펙이 통과한 뒤** 그 결정대로 코드를 쓴다. 새 결정이 필요해지면 멈추고 `spec-write` 로 돌아간다 — 코드 쓰면서 설계 결정을 즉흥적으로 바꾸지 않는다.

## 1. 언제 쓰는가

- 새 API 엔드포인트, 서비스 비즈니스 로직, DTO, JPA 엔티티/리포지토리/쿼리
- 트랜잭션 경계, custom exception, 단위/슬라이스/통합 테스트
- 기능 단위 리팩토링

## 2. 안 쓰는 곳

- 완료된 PR 리뷰 → `code-review`
- 머지 준비 검토 → `code-review`
- 디버깅 → `code-review`(의 디버깅 절차)
- 패턴 적용 결정 → `spec-write`

## 3. 핵심 원칙

- 스펙대로 쓴다 — 스펙에 없는 추상화 추가 금지
- 가장 단순한 동작 코드부터
- 추측 기반 확장점 추가 금지

## 4. 구현 순서

1. API 계약 (controller signature + DTO)
2. Service 비즈니스 로직
3. Repository 메서드/쿼리
4. 트랜잭션 경계 결정 적용
5. 예외/에러 응답 매핑
6. 테스트 추가
7. 머지 준비도 자체 확인

## 5. Controller

- HTTP method 와 의미 일치 (GET=조회, POST=생성/명령, PUT=전체교체, PATCH=부분, DELETE=삭제)
- GET 으로 상태 변경 금지
- thin controller — 비즈니스 로직은 Service 로 위임
- request DTO 검증 (`@Valid`)
- entity 직접 노출 금지 — response DTO 로 감싼다
- Repository 직접 호출 금지

## 6. DTO

- request/response 분리
- 사용처 필요에 맞춰 필드 구성
- entity 내부 구조 누출 금지
- 가능하면 불변
- 사용되지 않는 future field 추가 금지

## 7. Service

- 비즈니스 결정은 Service 레이어
- 한 메서드 = 한 use case
- 책임 섞인 큰 메서드 분해
- 도메인 행위를 util 클래스로 빼내지 않는다
- 메서드 이름이 의도를 드러내게

## 8. JPA / Repository

### N+1 방지

```kotlin
// ❌ N+1
val authors = authorRepository.findAll()  // 1 쿼리
authors.forEach { it.books.size }          // N 쿼리

// ✅ JOIN FETCH
@Query("SELECT a FROM Author a JOIN FETCH a.books")
fun findAllWithBooks(): List<Author>

// ✅ @EntityGraph
@EntityGraph(attributePaths = ["books"])
fun findAll(): List<Author>
```

### LazyInitializationException 방지

- 트랜잭션 밖에서 lazy 필드 접근 → 예외
- 해결: 쿼리에서 JOIN FETCH, Service 에 `@Transactional(readOnly = true)`, 또는 DTO projection

### 기타

- `@ManyToOne` 기본이 EAGER — 명시적으로 LAZY 로 덮어쓴다
- 대량 조회는 페이지네이션 또는 projection
- 양방향 연관관계는 정말 필요할 때만
- 커스텀 쿼리는 가독성 우선
- 쿼리 조건이 인덱스를 요구하면 인덱스를 추가

## 9. 트랜잭션

- 원자적 쓰기는 Service 메서드에 `@Transactional`
- 읽기 전용 메서드는 `@Transactional(readOnly = true)`
- **외부 API 호출은 트랜잭션 밖**으로 분리 — 락 점유 시간 길어지면 동시성 망가짐
- 다단 쓰기는 한 트랜잭션 안에서 모두 성공/모두 실패
- 트랜잭션 후 실행이 필요한 후속 작업은 `@TransactionalEventListener`
- 비동기 이벤트는 실패 처리/로깅 같이 설계

## 10. Exception

- 예상 가능한 비즈니스 실패는 custom exception
- 원인 예외 보존 (cause 체인)
- swallow 금지, 광범위 `Exception` catch 금지 (정당한 이유 없으면)
- 일관된 에러 응답으로 매핑 (`GlobalExceptionHandler`)
- validation / not-found / conflict 를 분리
- 내부 디테일을 API 응답에 노출 금지

## 11. Test

### 추가 대상

- 도메인 규칙/불변식
- 버그 수정 → 실패 테스트 먼저
- converter / parser / calculator / mapper / date·time
- 컨트롤러 계약 (상태 코드, 응답 본문, validation, 에러 매핑)
- 커스텀 쿼리, fetch join, 필터/정렬/페이지네이션
- 트랜잭션 민감 로직 (다단 쓰기 원자성)
- 보안 규칙 (auth, ownership)

### 스킵 가능

- 단순 필드 추가
- trivial getter/setter/생성자/enum 추가
- 기존 통합 테스트가 커버하는 프레임워크 wiring
- 스킵하면 수동 검증 방법을 명시

### 모양

- AAA (Arrange / Act / Assert)
- 메서드 이름이 동작과 기대 결과를 드러내게
- `@Test` 바로 위 한 줄 주석으로 비즈니스 의도

```java
// 반복 예약에 반복 요일이 없으면 예약 생성 시점에 실패하는지 확인한다.
@Test
void builderThrowsWhenRepeatDaysIsMissing() { ... }
```

### 무엇을 assert 하나

- 관찰 가능한 행위/상태
- 예외 타입과 메시지 (계약의 일부일 때)
- 컨트롤러: HTTP 상태, 에러 코드, 응답 모양
- 내부 구현 detail 은 assert 하지 않는다

### 데이터

- 최소·명시적
- setup 반복이 의도를 가릴 때만 helper
- 행위에 영향 주는 값(날짜, 좌표, 상태)은 현실적인 값
- 같은 규칙이 여러 입력에서 검증되면 `@ParameterizedTest`

### 범위

- 행위를 증명하는 **최소 범위**: unit > slice > integration 순으로 시도

### 실행

- 코드/테스트 변경 후 `./gradlew test` 또는 `./gradlew harness`
- 환경 한계로 못 돌리면 정확히 무엇이 미검증인지 보고

## 12. 패턴 적용

스펙에서 결정된 패턴만 적용한다. 코드 쓰는 중 새 패턴이 필요해 보이면:
1. 정말 필요한지 다시 묻는다 (단순 `new`/메서드 추출로 안 되나?)
2. 필요하면 멈추고 `spec-write` 로 돌아가 결정을 추가한다
3. 코드 안에 즉흥적으로 추상화하지 않는다

## 13. 머지 준비도 자체 확인

코드 작성 끝나면 다음을 자가 점검:

- API 계약이 스펙과 일치
- entity 노출 없음
- 트랜잭션 경계가 결정대로
- 예외 매핑 일관
- N+1 위험 없음
- 의미 있는 테스트 추가됨
- 스펙에 없는 추상화 추가하지 않음

## 14. 출력 형식

```
# Implementation — <기능 이름>

## 변경 파일
- controller / DTO / service / repository / entity / exception / test

## 핵심 코드 요약
- <레이어>: <한 줄>

## 자체 머지 체크
- [ ] API 계약 일치
- [ ] entity 미노출
- [ ] 트랜잭션 경계
- [ ] 예외 매핑
- [ ] N+1 없음
- [ ] 테스트 추가
- [ ] 추가된 추상화 = 스펙 결정
```

## 15. 한 줄 요약

**스펙대로 가장 단순한 동작 코드를 쓰고, 의미 있는 테스트로 막고, 새 결정이 필요해지면 코드 안에서 결정하지 말고 스펙으로 돌아간다.**
