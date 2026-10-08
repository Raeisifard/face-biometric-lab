# Phase M19 — Testing CI

## Mission
Build unit, bridge, orchestration, contract, UI-state and integration tests; mocked backend tests and instrumentation where practical. CI must run Flutter analysis/tests, Android tests/build, formatting, secret scanning and unintended large-asset checks.

## Required work
1. Read current backend/simulator contracts and all previous mobile outputs.
2. Implement only this phase while preserving later extension points.
3. Add automated tests and documentation; do not commit secrets or personal biometric data.
4. Keep Flutter presentation separate from Kotlin camera/biometric infrastructure.

## Acceptance criteria
CI blocks mobile regressions and hardware-dependent tests are explicitly separated and documented.

## Deliverables
- Production-quality implementation.
- Tests and required CI/build updates.
- Short report under docs/.
- Commit beginning with feat(mobile): M19 - (or docs(mobile): M19 - for documentation-only work).

## Stop condition
Do not start the next phase until acceptance criteria and the phase commit are complete.
