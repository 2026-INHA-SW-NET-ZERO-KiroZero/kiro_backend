---
name: pre-push-check
description: Use right before pushing one or more commits to remote. Trigger when the user says "push it", "is this ready to push?", "run pre-push", or when the pre-push hook asks for Claude verification. Does two heavier checks that pre-commit deliberately skipped: (1) make sure the test suite actually passes, and (2) make sure any code change that affects documented behavior is reflected in `docs/` (especially `docs/PRD/api-spec.md`). May propose doc updates; never updates code automatically.
---

# Pre-Push Check

Run right before `git push`. Looks at **all commits that are about to be pushed** (`origin/<branch>..HEAD`) and verifies two things.

Pre-commit stays light. This step is allowed to be slower because pushing is a less frequent action and the cost of pushing a broken build is high.

## 1. When to run

- The user says "push it" / "is this ready to push?" — run this **before** `git push`
- `.githooks/pre-push` triggers and the user asks Claude to verify
- The user explicitly asks "double-check before I push"

If there is nothing to push (`git log origin/<branch>..HEAD` is empty), exit immediately.

## 2. The two checks

### Check 1 — Tests pass

**Why this matters**: `./gradlew harness` already runs from `.githooks/pre-push`, but it can be skipped (`--no-verify`) or fail silently in CI later. Claude's job here is to **interpret the result**, not just trust an exit code.

Procedure:

1. Run `./gradlew test --daemon` (or `./gradlew harness` if the user wants the full pipeline including spotless + ArchUnit).
2. Wait for the run to finish. Do not assume success from a partial log.
3. Read the final summary:
   - All tests green → pass this check.
   - Failures → list each failing test class + method + the first useful line of the failure. Block the push.
   - Build errors (compile failures, missing deps) → report exactly which file/line and block.
4. If a test was **newly added in the pushed commits** but skipped (`@Disabled`, `@Ignore`), flag it. Reaching push time with disabled new tests is almost always unintentional.

**Judgment rule**: do not retry on flaky failures automatically. Report the failure and let the user decide.

### Check 2 — Docs stay in sync with code

**Why this matters**: this repo keeps product / API documentation under `docs/` (notably `docs/PRD/api-spec.md` and `docs/PRD/idea.md`). When code changes the documented behavior but the doc is not updated, the doc rots silently.

Procedure:

1. Get the diff for the commits about to be pushed:
   ```bash
   git diff origin/<branch>..HEAD
   ```
2. Identify code changes that **could affect documented behavior**:
   - New / changed / removed REST endpoints (controllers, `@RequestMapping`, `@GetMapping`, etc.)
   - Changed request / response DTO fields
   - Changed error codes or response envelope shape
   - New domain concepts or renamed entities that are referenced in `docs/PRD/idea.md`
3. For each such change, check whether the corresponding doc was updated **in the same push**:
   - REST surface changes → must be reflected in `docs/PRD/api-spec.md`
   - Product-level concept changes → must be reflected in `docs/PRD/idea.md`
4. For every mismatch found:
   - Point out the code change (file + line) and the doc section that is now stale.
   - **Propose a concrete doc edit** (the exact lines to add / change in the doc file).
   - Ask the user to confirm before applying. Apply only after confirmation.
5. If the docs were already updated in the same push, say so explicitly and move on.

**Judgment rule**: only flag changes that affect **externally observable behavior**. Internal refactors (renaming a private method, splitting a service) do not need doc updates.

## 3. Output format

If both checks pass:

```
✅ pre-push check passed
  - Tests: 142 passed, 0 failed
  - Docs: in sync (no public-surface change, or already updated in this push)
```

If something is off:

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

## 4. Do NOT do

- **No secret / commit-message check** — that already ran in pre-commit. Do not re-do it.
- **No automatic code fixes** — fixing failing tests is the user's job.
- **No silent doc edits** — always show the proposed diff and wait for confirmation.
- **No retry on flaky tests** — surface the failure honestly.
- **No scope creep** — do not touch docs that have nothing to do with the pushed changes.

## 5. One-line summary

**Pre-commit was the fast lane. Pre-push is the slow lane — tests must actually pass, and docs must actually match the code.**
