# Phase M16 — Security

## Mission
Harden TLS/network policy, authenticated-session hooks, nonce/correlation binding, replay-prevention hooks, secure configuration storage, secret exclusion, log redaction, model integrity/version policy and explicit Client Embedding limitations. Do not invent fake production authentication.

## Required work
1. Read current backend/simulator contracts and all previous mobile outputs.
2. Implement only this phase while preserving later extension points.
3. Add automated tests and documentation; do not commit secrets or personal biometric data.
4. Keep Flutter presentation separate from Kotlin camera/biometric infrastructure.

## Acceptance criteria
Security review documents threats, mitigations, remaining demo limitations and production follow-ups; repository contains no secrets.

## Deliverables
- Production-quality implementation.
- Tests and required CI/build updates.
- Short report under docs/.
- Commit beginning with feat(mobile): M16 - (or docs(mobile): M16 - for documentation-only work).

## Stop condition
Do not start the next phase until acceptance criteria and the phase commit are complete.
