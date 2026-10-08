# M00 — Architecture Phase Report

## Audit result

**Previous state:** M00 had been planned, but it had not been executed as a distinct phase.

The repository history showed the mobile roadmap/prompt commit, followed by the first Flutter application run. There was no M00 implementation/report commit and no architecture deliverable satisfying the M00 acceptance criteria.

Therefore the earlier state is classified as:

**M00 = NOT ACCEPTED / MISSING**

## Evidence reviewed

The audit checked:

- `face_mobile_demo/prompts/00-MOBILE-ARCHITECTURE.md`
- `docs/mobile-demo-roadmap.md`
- `face-biometric-service` verification controllers/contracts
- `docs/verification-response-contract.md`
- `docs/client-embedding.md`
- `docs/phase-06-policy-engine.md`
- `models/README.md`
- `face-client-simulator` model/configuration and API surface
- current `face_mobile_demo` project state

The roadmap commit added the M00 prompt and roadmap, but did not implement M00. The first mobile-run commit also did not constitute M00 because it only established/runs the Flutter project and did not provide the required architecture contract.

## Work completed in this M00 correction

- Added `face_mobile_demo/docs/M00-mobile-architecture.md`.
- Added the complete Flutter/Kotlin/native-ML/backend responsibility model.
- Mapped Full Clip, Server Live Stream, Client Embedding, Hybrid Best Frame and Hybrid Multi Frame to existing backend contracts.
- Documented the MobileFaceNet `w600k_mbf` 512-D contract and the server `w600k_r50` distinction.
- Documented the YuNet and MiniFASNetV2 roles.
- Defined the MethodChannel boundary and package structure.
- Defined configuration, diagnostics, dependency/version policy, Android baseline policy and CI gates.
- Defined the security trust boundary and production extension points.
- Defined dependencies for M01–M20.
- Moved the mobile roadmap into `face_mobile_demo/docs/` so mobile phase documentation has one canonical location.
- Kept the Windows/browser `face-client-simulator` independent.

## Scope check

No camera, ONNX, liveness, REST execution or verification-method implementation was added by M00.

M01 remains the next implementation phase.

## Acceptance status

**M00: READY FOR ACCEPTANCE**

The phase is now represented by an auditable architecture document and report. The commit must be merged into `master` before M00 is considered recorded.

## Next gate

After this M00 commit is visible on `master`, proceed to M01. M01 must still pass its own build, analyze, test and Android launch gates before M02.
