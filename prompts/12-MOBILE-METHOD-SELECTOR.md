# Phase M12 — Method Selector

## Mission
Create the method orchestration layer and selector for Full Clip, Server Live Stream, Client Embedding, Hybrid Best Frame and Hybrid Multi Frame, with future Adaptive/Policy support. Use one common VerificationSession/Result contract and method adapters.

## Required work
1. Read the current face-biometric-service, face-client-simulator, relevant docs and previous phase output.
2. Implement only this phase and preserve extension points for later phases.
3. Reuse existing API/model contracts; explicitly document backend contract changes.
4. Add tests and documentation; never commit secrets or personal biometric data.
5. Keep Flutter presentation separate from Kotlin camera/biometric infrastructure.

## Acceptance criteria
Switching methods does not duplicate the screen flow; all implemented methods share one lifecycle; unsupported methods are clearly disabled/explained.

## Deliverables
- Production-quality implementation.
- Required tests and CI/build updates.
- Short report under docs/.
- Commit beginning with feat(mobile): M12 - (or docs(mobile): M12 - for documentation-only work).

## Stop condition
Do not start the next phase until this phase is accepted and its commit is recorded.
