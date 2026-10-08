# Phase M15 — Failure Recovery

## Mission
Implement robust camera/permission denial, no face, poor quality, liveness failure, timeout, HTTP/service failure, malformed response, cancellation, backgrounding and retry/restart flows. Define user-safe messages and machine-readable diagnostics. Never leave UI stuck in processing.

## Required work
1. Read current backend/simulator contracts and all previous mobile outputs.
2. Implement only this phase while preserving later extension points.
3. Add automated tests and documentation; do not commit secrets or personal biometric data.
4. Keep Flutter presentation separate from Kotlin camera/biometric infrastructure.

## Acceptance criteria
Every defined failure state has a deterministic recovery path and repeated sessions never retain stale state.

## Deliverables
- Production-quality implementation.
- Tests and required CI/build updates.
- Short report under docs/.
- Commit beginning with feat(mobile): M15 - (or docs(mobile): M15 - for documentation-only work).

## Stop condition
Do not start the next phase until acceptance criteria and the phase commit are complete.
