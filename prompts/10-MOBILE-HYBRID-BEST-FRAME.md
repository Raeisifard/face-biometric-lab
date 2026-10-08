# Phase M10 — Hybrid Best Frame

## Mission
Implement Hybrid Best Frame: collect candidates, score quality/liveness/readiness, select the best valid frame and send the backend method-specific payload. Reuse the common capture pipeline.

## Required work
1. Read the current face-biometric-service, face-client-simulator, relevant docs and previous phase output.
2. Implement only this phase and preserve extension points for later phases.
3. Reuse existing API/model contracts; explicitly document backend contract changes.
4. Add tests and documentation; never commit secrets or personal biometric data.
5. Keep Flutter presentation separate from Kotlin camera/biometric infrastructure.

## Acceptance criteria
The app reliably selects a valid best frame and completes end-to-end hybrid verification with correct result mapping.

## Deliverables
- Production-quality implementation.
- Required tests and CI/build updates.
- Short report under docs/.
- Commit beginning with feat(mobile): M10 - (or docs(mobile): M10 - for documentation-only work).

## Stop condition
Do not start the next phase until this phase is accepted and its commit is recorded.
