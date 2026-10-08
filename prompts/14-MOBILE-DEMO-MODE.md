# Phase M14 — Demo Mode

## Mission
Turn the app into a customer-demo product. Add Demo Mode controls for verification method, reference ID, server environment/URL, permitted thresholds, diagnostics and reset. Add scripted demo presets and a polished Customer Mode with minimal controls. Configuration must not silently weaken production security.

## Required work
1. Read current backend/simulator contracts and all previous mobile outputs.
2. Implement only this phase while preserving later extension points.
3. Add automated tests and documentation; do not commit secrets or personal biometric data.
4. Keep Flutter presentation separate from Kotlin camera/biometric infrastructure.

## Acceptance criteria
A presenter can switch scenarios quickly, run a complete demonstration without developer tools, and return to Customer Mode cleanly.

## Deliverables
- Production-quality implementation.
- Tests and required CI/build updates.
- Short report under docs/.
- Commit beginning with feat(mobile): M14 - (or docs(mobile): M14 - for documentation-only work).

## Stop condition
Do not start the next phase until acceptance criteria and the phase commit are complete.
