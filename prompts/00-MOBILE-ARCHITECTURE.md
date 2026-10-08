# Phase M00 — Architecture

## Mission
Define the complete mobile architecture before coding: Flutter UI + Kotlin Android biometric engine; CameraX; ONNX Runtime Mobile; YuNet; MiniFASNetV2; MobileFaceNet; REST client; Flutter↔Kotlin typed platform API; model assets; configuration; logging; security boundary; dependency/version policy; minimum Android SDK; CI; package structure. Map every verification method to existing backend endpoints/contracts. Keep face-client-simulator independent.

## Required work
1. Read the current `face-biometric-service`, `face-client-simulator`, relevant docs and previous phase output before coding.
2. Implement only this phase while preserving extension points required later.
3. Reuse existing API/model contracts; document any backend contract change explicitly.
4. Add appropriate automated tests and documentation.
5. Never commit secrets, customer data or personal biometric data.
6. Keep Flutter presentation separate from Kotlin camera/biometric infrastructure.

## Acceptance criteria
Architecture document exists, dependencies and boundaries are explicit, backend contract mapping is complete, model contract is explicit, and all later phase dependencies are defined.

## Deliverables
- Production-quality implementation for this phase.
- Tests and CI/build updates required by the phase.
- A short report under `docs/`.
- A Git commit beginning with `feat(mobile): M00 -` (or `docs(mobile): M00 -` for documentation-only work).

## Stop condition
Do not start the next phase until this phase is accepted and its commit is recorded.
