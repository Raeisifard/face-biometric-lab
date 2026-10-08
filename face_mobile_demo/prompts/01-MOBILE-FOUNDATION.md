# Phase M01 — Foundation

## Mission
Create `face-mobile-demo/` as a Flutter project with Android/Kotlin native bridge, clean package structure, app identity, build configuration, environment configuration, lint/format rules, typed platform API, placeholder biometric engine and minimal launchable screen. No camera/ML yet.

## Required work
1. Read the current `face-biometric-service`, `face-client-simulator`, relevant docs and previous phase output before coding.
2. Implement only this phase while preserving extension points required later.
3. Reuse existing API/model contracts; document any backend contract change explicitly.
4. Add appropriate automated tests and documentation.
5. Never commit secrets, customer data or personal biometric data.
6. Keep Flutter presentation separate from Kotlin camera/biometric infrastructure.

## Acceptance criteria
Debug APK builds and launches; Flutter calls Kotlin and receives a typed health/status response; clean-machine setup is documented.

## Deliverables
- Production-quality implementation for this phase.
- Tests and CI/build updates required by the phase.
- A short report under `docs/`.
- A Git commit beginning with `feat(mobile): M01 -` (or `docs(mobile): M01 -` for documentation-only work).

## Stop condition
Do not start the next phase until this phase is accepted and its commit is recorded.
