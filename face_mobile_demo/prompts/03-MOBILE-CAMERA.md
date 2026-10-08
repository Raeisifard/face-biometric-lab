# Phase M03 — Camera

## Mission
Integrate CameraX in Kotlin: lifecycle-safe preview, frame analysis, front camera, rotation/mirroring, configurable resolution/FPS, permissions, resource shutdown and a stable frame-analysis stream.

## Required work
1. Read the current `face-biometric-service`, `face-client-simulator`, relevant docs and previous phase output before coding.
2. Implement only this phase while preserving extension points required later.
3. Reuse existing API/model contracts; document any backend contract change explicitly.
4. Add appropriate automated tests and documentation.
5. Never commit secrets, customer data or personal biometric data.
6. Keep Flutter presentation separate from Kotlin camera/biometric infrastructure.

## Acceptance criteria
Physical-device preview and analysis are stable across lifecycle/orientation changes with bounded memory and no leaks.

## Deliverables
- Production-quality implementation for this phase.
- Tests and CI/build updates required by the phase.
- A short report under `docs/`.
- A Git commit beginning with `feat(mobile): M03 -` (or `docs(mobile): M03 -` for documentation-only work).

## Stop condition
Do not start the next phase until this phase is accepted and its commit is recorded.
