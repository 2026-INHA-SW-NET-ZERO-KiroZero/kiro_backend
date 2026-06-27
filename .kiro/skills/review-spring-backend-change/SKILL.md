---
name: review-spring-backend-change
description: Review Spring Boot PR/diff for API, service, JPA, transaction, exception, test, and merge-readiness risks. Not for implementation.
---
# Review Spring Boot PR
Use this skill when reviewing an existing Spring Boot PR, diff, or code change.
## 1. Use This For
- Backend PR review before merge
- API contract review
- Controller and service responsibility review
- JPA entity, repository, and query review
- Transaction boundary review
- Exception handling review
- Null safety review
- Test coverage review
- Merge-readiness review
## 2. Do Not Use This For
- Implementing a new feature
- Writing full production code
- Designing architecture without a diff
- Applying patterns proactively
- Reviewing unrelated frontend code
- General Spring Boot explanation
## 3. Core Principle
Review correctness and merge risk before style.
Focus on broken APIs, data inconsistency, runtime errors, performance issues, and missing tests.
Do not request abstraction unless it solves a visible problem in the diff.
## 4. Review Flow
1. Understand PR intent.
2. Identify API behavior changes.
3. Check controller responsibility.
4. Check service business logic.
5. Check repository and JPA usage.
6. Check transaction safety.
7. Check exception handling.
8. Check tests.
9. Decide merge-readiness.
## 5. API Contract
- Is the HTTP method appropriate?
- Is the URL resource-oriented?
- Are request and response DTOs stable?
- Are status codes correct?
- Are validation rules explicit?
- Are breaking changes documented?
- Are nullable fields intentional?
- Are response fields consistent?
- Is pagination needed?
- Are entities exposed directly?
## 6. Controller
- Is the controller thin?
- Does it avoid business logic?
- Does it validate request DTOs?
- Does it delegate to services?
- Does it avoid repository access?
- Does it return consistent response types?
- Does it avoid duplicated mapping logic?
- Are path variables and request params clear?
## 7. Service
- Is the use case clear?
- Is business logic in the service layer?
- Are method names meaningful?
- Is branching logic readable?
- Are side effects intentional?
- Are external calls handled safely?
- Is duplicated logic introduced?
- Is abstraction justified by this change?
- Are domain rules enforced consistently?
## 8. JPA / Repository
- Is there N+1 query risk?
- Is pagination used for large reads?
- Are fetch joins or entity graphs needed?
- Are custom queries correct?
- Are repository methods named clearly?
- Are entity relationships necessary?
- Are cascade options safe?
- Are indexes needed?
- Is lazy loading used safely?
## 9. Transaction
- Are multi-step writes transactional?
- Is `@Transactional` placed at service level?
- Are read-only methods marked when useful?
- Are external API calls inside transactions avoided?
- Is event timing safe?
- Can partial writes occur on failure?
- Is rollback behavior correct?
## 10. Exception
- Are expected failures represented by custom exceptions?
- Are not-found, validation, and conflict cases separated?
- Are exceptions mapped to proper HTTP responses?
- Is the original cause preserved?
- Are exceptions logged at the right layer?
- Are broad catches justified?
- Are internal details hidden from API responses?
## 11. Test
- Is the success path tested?
- Are validation failures tested?
- Are not-found cases tested?
- Are conflict cases tested?
- Are transaction-sensitive cases tested?
- Are custom repository queries tested?
- Are controller contracts tested?
- Are edge cases covered?
## 12. Merge Decision
- Must fix: data loss, security issue, runtime crash, broken API, missing critical transaction.
- Should fix: N+1 risk, missing important test, unclear exception mapping, duplicated business logic.
- Follow up: naming polish, minor style, small readability improvement.
## 13. Response Format
- PR Review Summary: intent, overall risk, merge decision
- Critical Issues: issue, impact, suggested fix
- Important Improvements: issue, reason, suggested fix
- Minor Comments: comment, optional improvement
- Missing Tests: case, why it matters
- Good Practices: positive point
- Final Decision: approve, request changes, or comment only