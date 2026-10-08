# Phase M09 — Client Embedding

## Mission
Implement local alignment and MobileFaceNet embedding with the documented w600k_mbf contract: 512-D, 112x112 RGB, specified normalization and L2 normalization. Send via the existing service contract. Treat embedding as untrusted evidence and preserve security metadata hooks.

## Required work
1. Read the current face-biometric-service, face-client-simulator, relevant docs and previous phase output.
2. Implement only this phase and preserve extension points for later phases.
3. Reuse existing API/model contracts; explicitly document backend contract changes.
4. Add tests and documentation; never commit secrets or personal biometric data.
5. Keep Flutter presentation separate from Kotlin camera/biometric infrastructure.

## Acceptance criteria
Android embeddings are contract-compatible with the existing simulator/backend and controlled known-match/non-match Client Embedding tests pass end-to-end.

## Deliverables
- Production-quality implementation.
- Required tests and CI/build updates.
- Short report under docs/.
- Commit beginning with feat(mobile): M09 - (or docs(mobile): M09 - for documentation-only work).

## Stop condition
Do not start the next phase until this phase is accepted and its commit is recorded.
