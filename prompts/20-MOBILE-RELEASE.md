# Phase M20 — Release

## Mission
Prepare release candidate and final demo package. Freeze contracts, update README/architecture docs, document environment setup, service/model setup, demo scripts, troubleshooting, limitations and production gaps. Produce reproducible demo APK and record exact version/build metadata.

## Required work
1. Read current backend/simulator contracts and all previous mobile outputs.
2. Implement only this phase while preserving later extension points.
3. Add automated tests and documentation; do not commit secrets or personal biometric data.
4. Keep Flutter presentation separate from Kotlin camera/biometric infrastructure.

## Acceptance criteria
Fresh setup reproduces the build; presenter can run every supported method; docs match behavior; final release checklist is complete.

## Deliverables
- Production-quality implementation.
- Tests and required CI/build updates.
- Short report under docs/.
- Commit beginning with feat(mobile): M20 - (or docs(mobile): M20 - for documentation-only work).

## Stop condition
Do not start the next phase until acceptance criteria and the phase commit are complete.
