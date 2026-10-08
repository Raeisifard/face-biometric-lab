# Phase M04 — Face Detection

## Mission
Integrate YuNet through ONNX Runtime Mobile. Define model loading, preprocessing, detection types, coordinate conversion, confidence thresholds and one-face policy; expose compact typed detection events to Flutter.

## Required work
1. Read the current `face-biometric-service`, `face-client-simulator`, relevant docs and previous phase output before coding.
2. Implement only this phase while preserving extension points required later.
3. Reuse existing API/model contracts; document any backend contract change explicitly.
4. Add appropriate automated tests and documentation.
5. Never commit secrets, customer data or personal biometric data.
6. Keep Flutter presentation separate from Kotlin camera/biometric infrastructure.

## Acceptance criteria
Live detection works on device; no-face/multi-face states are correct; model loading and preprocessing have automated coverage where practical.

## Deliverables
- Production-quality implementation for this phase.
- Tests and CI/build updates required by the phase.
- A short report under `docs/`.
- A Git commit beginning with `feat(mobile): M04 -` (or `docs(mobile): M04 -` for documentation-only work).

## Stop condition
Do not start the next phase until this phase is accepted and its commit is recorded.
