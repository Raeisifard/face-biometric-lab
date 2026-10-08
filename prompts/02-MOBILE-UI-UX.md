# Phase M02 — UI UX

## Mission
Build a product-like UI shell and design system: Verify Identity, camera placeholder, processing, result, settings/demo controls, method selection, reference selection, help/about, accessibility. Separate Customer Mode and Demo/Developer Mode. Keep biometric logic out of widgets.

## Required work
1. Read the current `face-biometric-service`, `face-client-simulator`, relevant docs and previous phase output before coding.
2. Implement only this phase while preserving extension points required later.
3. Reuse existing API/model contracts; document any backend contract change explicitly.
4. Add appropriate automated tests and documentation.
5. Never commit secrets, customer data or personal biometric data.
6. Keep Flutter presentation separate from Kotlin camera/biometric infrastructure.

## Acceptance criteria
All navigation and states are usable without ML/backend; Match/No Match and processing states are visually clear and deterministic.

## Deliverables
- Production-quality implementation for this phase.
- Tests and CI/build updates required by the phase.
- A short report under `docs/`.
- A Git commit beginning with `feat(mobile): M02 -` (or `docs(mobile): M02 -` for documentation-only work).

## Stop condition
Do not start the next phase until this phase is accepted and its commit is recorded.
