# Phase M17 — Performance

## Mission
Profile camera FPS, frame copies, allocations, ONNX inference, model initialization, network payloads, battery/thermal behavior and memory. Use bounded queues/backpressure and minimize Flutter↔Kotlin serialization. Define measurable budgets by device class.

## Required work
1. Read current backend/simulator contracts and all previous mobile outputs.
2. Implement only this phase while preserving later extension points.
3. Add automated tests and documentation; do not commit secrets or personal biometric data.
4. Keep Flutter presentation separate from Kotlin camera/biometric infrastructure.

## Acceptance criteria
Defined performance budgets are measured on representative devices and optimizations do not break biometric contract or UX.

## Deliverables
- Production-quality implementation.
- Tests and required CI/build updates.
- Short report under docs/.
- Commit beginning with feat(mobile): M17 - (or docs(mobile): M17 - for documentation-only work).

## Stop condition
Do not start the next phase until acceptance criteria and the phase commit are complete.
