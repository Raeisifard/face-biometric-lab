# Phase M11 — Hybrid Multi Frame

## Mission
Implement Hybrid Multi Frame with bounded candidate set, explicit selection/aggregation policy, payload limits, ordering, cancellation and existing server contract. Keep detailed diagnostics in Demo Mode.

## Required work
1. Read the current face-biometric-service, face-client-simulator, relevant docs and previous phase output.
2. Implement only this phase and preserve extension points for later phases.
3. Reuse existing API/model contracts; explicitly document backend contract changes.
4. Add tests and documentation; never commit secrets or personal biometric data.
5. Keep Flutter presentation separate from Kotlin camera/biometric infrastructure.

## Acceptance criteria
Multi-frame verification works within defined payload/time limits and is deterministic for controlled fixtures.

## Deliverables
- Production-quality implementation.
- Required tests and CI/build updates.
- Short report under docs/.
- Commit beginning with feat(mobile): M11 - (or docs(mobile): M11 - for documentation-only work).

## Stop condition
Do not start the next phase until this phase is accepted and its commit is recorded.
