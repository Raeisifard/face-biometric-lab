# Phase M08 — Server Live Stream

## Mission
Integrate Server Live Stream using the existing backend contract. Implement bounded streaming/chunking, backpressure, cancellation, session lifecycle and responsive processing UI.

## Required work
1. Read the current face-biometric-service, face-client-simulator, relevant docs and previous phase output.
2. Implement only this phase and preserve extension points for later phases.
3. Reuse existing API/model contracts; explicitly document backend contract changes.
4. Add tests and documentation; never commit secrets or personal biometric data.
5. Keep Flutter presentation separate from Kotlin camera/biometric infrastructure.

## Acceptance criteria
Live Stream works on a physical device against the service and remains responsive under slow server/network conditions.

## Deliverables
- Production-quality implementation.
- Required tests and CI/build updates.
- Short report under docs/.
- Commit beginning with feat(mobile): M08 - (or docs(mobile): M08 - for documentation-only work).

## Stop condition
Do not start the next phase until this phase is accepted and its commit is recorded.
