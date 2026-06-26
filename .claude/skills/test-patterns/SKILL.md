---
name: test-patterns
description: Apply practical Spring backend testing principles focused on important behavior, readable tests, and minimal useful coverage.
---

# Test Patterns

Use this skill when adding, reviewing, or refactoring tests in this Spring Boot project.

## 1. Core Principle
Test important behavior, not every line of code.
Add tests when a change introduces business rules, branching, parsing, persistence mapping,
validation, exception handling, transaction behavior, or a bug fix.
Do not create tests only to increase coverage numbers.

## 2. When To Add Tests
Add tests for domain rules and invariants.
Add tests for bug fixes, preferably with a failing test first.
Add tests for converters, parsers, calculators, mappers, and time/date logic.
Add tests for controller contracts when status code, response body, validation, or error mapping matters.
Add tests for repository queries when custom queries, fetch joins, filtering, sorting, or pagination are used.
Add tests for transaction-sensitive logic when multiple writes must succeed or fail together.
Add tests for security rules when authentication, authorization, or ownership checks are involved.

## 3. When Tests May Be Skipped
You may skip dedicated tests for simple field additions with no behavior change.
You may skip tests for trivial getters, setters, constructors, or enum additions.
You may skip tests for framework wiring if existing startup or integration tests already cover it.
If skipping tests for a risky change, explain the manual verification instead.

## 4. Test Shape
Prefer the AAA structure: Arrange, Act, Assert.
Use descriptive method names that explain behavior and expected outcome.
Place a short comment directly above each `@Test` explaining the test purpose.
Keep the comment one sentence and focused on business intent, not implementation details.
Example:

```java
// 반복 예약에 반복 요일이 없으면 예약 생성 시점에 실패하는지 확인한다.
@Test
void builderThrowsWhenRepeatDaysIsMissing() {
    ...
}
```

## 5. Test Scope
Use plain unit tests for entities, value objects, converters, validators, and pure service logic.
Use slice tests for controller behavior or repository behavior when a full application context is unnecessary.
Use integration tests when Spring wiring, transactions, JPA mapping, or database behavior is part of the risk.
Prefer the smallest test scope that proves the behavior.

## 6. What To Assert
Assert observable behavior and state.
Assert exception type and message when the message is part of the contract or helps identify the failed rule.
Assert API status code, error code, and response shape for controller and exception-handler tests.
Do not assert internal implementation details unless they are the behavior under test.

## 7. Test Data
Keep test data minimal and explicit.
Use helper methods only when setup repetition hides the intent of the test.
Avoid large fixture objects when a small builder setup is enough.
Use realistic values for dates, coordinates, IDs, and enum states when they affect behavior.

## 8. Reducing Duplication
If several tests share the same valid object setup, use a small private helper method.
Use `@ParameterizedTest` when the same rule is checked across multiple inputs.
Do not over-generalize test helpers; tests should remain easy to read locally.

## 9. Verification
Run `./gradlew test` after changing production code or test code.
If tests cannot run because of environment limits, report the exact failure and what remains unverified.
