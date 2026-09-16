# Phase 00 — Biometric Foundation Stabilization

This phase stabilizes the shared 1:1 verification foundation without implementing the later capture/recognition methods.

## Structure

`face-biometric-service` keeps the existing API, repository and matching code and adds a small `biometric` contract layer:

- `FaceDetector`
- `FaceAligner`
- `FaceQualityAnalyzer`
- `LivenessDetector`
- `FaceEmbeddingModel`
- `FaceMatcher`
- `ReferenceEmbeddingRepository`
- `BiometricVerificationService`

The existing `FaceEmbeddingRepository` now extends `ReferenceEmbeddingRepository`, so current property/memory/database implementations can be reused.

## REST foundation

`POST /api/v1/biometric/verify` is the stable 1:1 contract. Phase 00 accepts `capture.type=EMBEDDING` so the existing synthetic/configured reference data can be exercised without implementing a new capture method.

The response uses `MATCH`, `NO_MATCH`, `INCONCLUSIVE`, `INVALID_REQUEST`, and reserves `PROCESSING_ERROR` for later processing failures.

`nonce` and `sessionId` are contract fields only; replay protection, session binding, client identity and device attestation are intentionally deferred.

## Configuration

The model path, model version, dimension, algorithm, threshold, normalization, detector and liveness settings are configurable. Detector/liveness are disabled by default. MongoDB and Oracle remain optional; Mongo auto-configuration is excluded by the existing application configuration.

The configured threshold is a development/test value and is **not** a banking suitability claim. Real biometric evaluation data and operating-point selection belong to a later evaluation phase.

## Manual verification

1. Start the service with `mvn spring-boot:run` from the repository root, or run `FaceBiometricServiceApplication` from the IDE.
2. Confirm `GET /api/v1/biometric/models` returns the configured model metadata.
3. Configure a reference embedding in `biometric-embeddings.yml` (or keep the existing test fixture).
4. Call `POST /api/v1/biometric/verify` with a 512-dimensional normalized embedding when using the default configuration.
5. Verify `MATCH`, `NO_MATCH`, and `INCONCLUSIVE` behavior.
6. Run `mvn test` and replace synthetic vectors with a controlled evaluation dataset when moving beyond the foundation phase.

## Deliberately deferred

Five capture methods, streaming, client-side recognition, policy engine, adaptive/fallback orchestration, production liveness, challenge/nonce enforcement, replay protection, client identity, device attestation, and production threshold calibration are not part of Phase 00.
