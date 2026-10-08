# Phase M18 — Compatibility Packaging

## Mission
Validate supported Android APIs, orientations, aspect ratios, low/mid/high-tier devices, offline transitions and ABI/model packaging. Produce debug/demo variants, versioning, permissions review, icons and installation instructions. Document model licensing/attribution.

## Required work
1. Read current backend/simulator contracts and all previous mobile outputs.
2. Implement only this phase while preserving later extension points.
3. Add automated tests and documentation; do not commit secrets or personal biometric data.
4. Keep Flutter presentation separate from Kotlin camera/biometric infrastructure.

## Acceptance criteria
Compatibility matrix is populated; release packaging is reproducible; supported devices install and launch reliably.

## Deliverables
- Production-quality implementation.
- Tests and required CI/build updates.
- Short report under docs/.
- Commit beginning with feat(mobile): M18 - (or docs(mobile): M18 - for documentation-only work).

## Stop condition
Do not start the next phase until acceptance criteria and the phase commit are complete.
