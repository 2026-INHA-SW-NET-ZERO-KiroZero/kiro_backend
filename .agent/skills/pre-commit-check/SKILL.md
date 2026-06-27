---
name: pre-commit-check
description: Use right before creating a commit to give the staged diff (`git diff --cached`) a lightweight human-reviewer pass. Trigger when the user says "commit this", "is this safe to commit?", "run pre-commit", or when the pre-commit hook asks for Claude verification. Only checks the two things that lint (spotless) cannot: (1) secret / sensitive-data leaks and (2) commit-message quality. Stays lightweight on purpose — this is a quick second look, not a full review.
---

# Pre-Commit Check

Run right before a commit is created. Looks **only at staged changes** and verifies two things that spotless / lint cannot catch.

The point of this skill is to act like a quick second pair of eyes for **contextual problems**, not to re-do what the linter already does.

## 1. When to run

- The user says "commit this" / "is this safe to commit?" — run this **before** creating the commit
- `.githooks/pre-commit` triggers and the user asks Claude to verify
- The user explicitly asks "take a quick look at what's staged"

If there are no staged changes (`git diff --cached --quiet` returns true), exit immediately and do nothing.

## 2. The two checks

### Check 1 — Secret / sensitive-data leak

**Why this matters**: regex-based tools (git-secrets, trufflehog) only catch tokens with a fixed shape. Secrets that are only recognizable by **variable name or surrounding context** slip through them.

Procedure:

1. Run `git diff --cached` and read the full staged diff.
2. Block the commit and report if any of the following appear:
   - **Named secrets with hardcoded values**: `apiKey = "..."`, `password = "..."`, `secret = "..."`, `token = "..."` where the value is real. Placeholder values like `"your-key-here"`, `"changeme"`, `"xxx"` are fine.
   - **Environment configs with real values**: `application-prod.yml`, `application-staging.yml`, etc. containing real DB URLs, passwords, or keys.
   - **`.env`-style files getting staged**: `.env`, `.env.prod`, `.env.local` — almost always a mistake.
   - **JWT / access-token shapes**: long base64 strings starting with `eyJ...`, or known prefixes like `sk-...`, `ghp_...`, `AKIA...`.
   - **Personal data**: real-looking emails, phone numbers, national IDs (clearly fake test fixtures are fine).
3. When something is found, point to the exact file and line and let the user unstage it themselves. **Do not unstage automatically.**

**Judgment rule**: if in doubt, ask the user. A false positive costs a few seconds; a leaked secret costs much more.

### Check 2 — Commit message quality

**Why this matters**: this repo uses conventional prefixes (`chore:`, `feat:`, `fix:`, `ci:`, ...). When the message doesn't match the actual change, `git log` becomes useless.

Procedure:

1. Take the commit message (final or draft) the user wants to use.
2. Run `git log --oneline -10` to confirm the prefix style currently in use — match it exactly.
3. Compare the message against the staged diff and check:
   - **Wrong prefix**: e.g. `chore:` used for what is actually a new feature (`feat:`) or a bug fix (`fix:`).
   - **Too vague**: one-word messages like `update`, `fix`, `wip`.
   - **Mismatch with the diff**: the message says "swagger setup" but the diff also touches controller logic.
4. If something is off, **do not block** — propose 1 or 2 better messages and let the user pick.

**Judgment rule**: if the message is obviously fine, just pass it. Do not nitpick.

## 3. Output format

Keep the report short. If both checks pass, one line:

```
✅ pre-commit check passed (no secrets, commit message OK)
```

If something is off, 1–3 lines per check:

```
❌ Possible secret leak
  - application-prod.yml:12 looks like a real DB password ("kiro_2026!")
  - Action: move it to an env var, unstage, then recommit

⚠️ Commit message suggestion
  - Current: "chore: response handling"
  - Suggested: "feat(global): add ApiResponse common wrapper" (the diff adds a new feature)
```

## 4. Do NOT do

- **No tests** — that belongs in pre-push. This step must stay fast.
- **No re-linting / re-formatting** — spotless already runs from `.githooks/pre-commit`.
- **No automatic fixes** — unstaging secrets or rewriting the message is the user's decision.
- **No style nitpicks** — naming, function length, comment style belong to the full `review` skill, not here.

## 5. One-line summary

**Lint checks shape. This skill checks context — only two things, secrets and message, kept short.**
