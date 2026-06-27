---
name: implement-spring-backend-feature
description: Implement Spring Boot backend features across API, service, JPA, transaction, exception, and tests. Not for PR review.
---
# Implement Spring Backend Feature
Use this skill when implementing or modifying a Spring Boot backend feature.
## 1. Use This For
- New API endpoints
- Service business logic
- Request / response DTOs
- JPA entities, repositories, or queries
- Transaction boundaries
- Custom exceptions and error responses
- Unit, slice, or integration tests
- Feature-level refactoring
## 2. Do Not Use This For
- Reviewing an already completed PR
- Only checking merge-readiness
- Only finding risks in a diff
- Pure style review
- Speculative architecture design
- Pattern application without a real problem
## 3. Core Principle
Start with the simplest working design.
Do not add Factory, Strategy, Event, Decorator, or Adapter unless the current requirement clearly needs it.
Prefer clear service logic over premature abstraction.
## 4. Implementation Flow
1. Define the feature goal.
2. Design the API contract.
3. Create request / response DTOs.
4. Implement service logic.
5. Add repository methods or JPA queries.
6. Decide transaction boundaries.
7. Define exceptions and error responses.
8. Add tests.
9. Check merge-readiness.
## 5. API / Controller
- Use correct REST semantics.
- Use `GET` for retrieval.
- Use `POST` for creation or commands.
- Use `PUT` for full replacement.
- Use `PATCH` for partial updates.
- Use `DELETE` for deletion.
- Do not use `GET` for state changes.
- Return proper HTTP status codes.
- Keep controllers thin.
- Delegate business logic to services.
- Validate request DTOs.
- Do not expose JPA entities.
## 6. DTO
- Separate request and response DTOs.
- Keep DTOs aligned with API needs.
- Do not leak internal entity structure.
- Use clear field names.
- Validate required values.
- Prefer immutable DTOs.
- Avoid unused future fields.
## 7. Service
- Put business decisions in the service layer.
- Keep one method focused on one use case.
- Avoid large methods with mixed responsibilities.
- Extract methods only when readability improves.
- Do not call repositories from controllers.
- Avoid hiding domain behavior in utility classes.
- Use meaningful method names.
## 8. JPA / Repository
- Design repository methods around use cases.
- Avoid loading all rows without pagination.
- Check N+1 query risks.
- Use fetch join or entity graph when needed.
- Keep custom queries readable.
- Use indexes when query conditions require them.
- Avoid unnecessary bidirectional relationships.
## 9. Transaction
- Use `@Transactional` for atomic write use cases.
- Use `@Transactional(readOnly = true)` for read-only service methods when useful.
- Keep transaction boundaries at service level.
- Avoid external API calls inside transactions.
- Ensure multi-step writes succeed or fail together.
- Consider transaction events when timing matters.
## 10. Exception
- Use custom exceptions for expected business failures.
- Preserve original causes when wrapping exceptions.
- Do not swallow exceptions.
- Avoid broad `Exception` catches.
- Map exceptions to consistent error responses.
- Separate validation, not-found, and conflict errors.
- Do not expose internal details to clients.
## 11. Test
- Test the success path.
- Test validation failures.
- Test not-found cases.
- Test conflict cases.
- Test transaction-sensitive behavior.
- Use unit tests for isolated service logic.
- Use slice tests for controllers or repositories.
- Use integration tests for cross-layer behavior.
## 12. Response Format
- Implementation Plan: goal, API, main flow, persistence, transaction, exceptions, tests
- Files to Change: controller, DTO, service, repository, entity, exception, test
- Merge Checklist: API contract, entity exposure, transaction, exception mapping, N+1 risk, tests, abstraction