# Phase M07 — Full Clip

## Mission
Integrate Full Clip against the existing service contract. Capture a bounded clip/frame set, encode exactly as required by the backend, upload reference/session metadata, show progress, support cancellation and map the response to the common result model.

## Required work
1. Read the current face-biometric-service, face-client-simulator, relevant docs and previous phase output.
2. Implement only this phase and preserve extension points for later phases.
3. Reuse existing API/model contracts; explicitly document backend contract changes.
4. Add tests and documentation; never commit secrets or personal biometric data.
5. Keep Flutter presentation separate from Kotlin camera/biometric infrastructure.

## Acceptance criteria
A physical Android device completes Full Clip end-to-end against face-biometric-service and returns correct Match/No Match plus actionable failures.

## Deliverables
- Production-quality implementation.
- Required tests and CI/build updates.
- Short report under docs/.
- Commit beginning with feat(mobile): M07 - (or docs(mobile): M07 - for documentation-only work).

## Stop condition
Do not start the next phase until this phase is accepted and its commit is recorded.
