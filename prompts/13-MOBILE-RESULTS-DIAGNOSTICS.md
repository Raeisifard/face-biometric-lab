# Phase M13 — Results Diagnostics

## Mission
Create unified results: Match, No Match, indeterminate/error, similarity when available, reference, method, latency, liveness outcome and server decision. Add optional timings/correlation/session diagnostics in Demo Mode and redact sensitive logs.

## Required work
1. Read the current face-biometric-service, face-client-simulator, relevant docs and previous phase output.
2. Implement only this phase and preserve extension points for later phases.
3. Reuse existing API/model contracts; explicitly document backend contract changes.
4. Add tests and documentation; never commit secrets or personal biometric data.
5. Keep Flutter presentation separate from Kotlin camera/biometric infrastructure.

## Acceptance criteria
Every method renders a consistent result and Demo Mode can explain the transaction without exposing secrets or unnecessary raw biometric data.

## Deliverables
- Production-quality implementation.
- Required tests and CI/build updates.
- Short report under docs/.
- Commit beginning with feat(mobile): M13 - (or docs(mobile): M13 - for documentation-only work).

## Stop condition
Do not start the next phase until this phase is accepted and its commit is recorded.
