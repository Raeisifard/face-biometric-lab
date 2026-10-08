# M00 — Mobile Architecture

## Status

**Phase:** M00 — Architecture  
**Repository:** `face-biometric-lab`  
**Mobile module:** `face_mobile_demo/`  
**Decision:** Accepted as the architectural baseline for M01–M20 once the M00 commit is merged.

This document is the implementation-independent architecture contract for the Android mobile demo. It was produced from the current `face-biometric-service`, `face-client-simulator`, model documentation, verification response contract, policy-engine documentation, and the mobile phase roadmap.

## 1. Architectural goals

The mobile demo must:

- provide a product-like Android experience using Flutter;
- keep camera, real-time frame processing, face detection, liveness and ONNX inference in native Kotlin;
- use CameraX for camera acquisition;
- use ONNX Runtime Mobile for on-device inference;
- reuse the repository's established YuNet, MiniFASNetV2 and MobileFaceNet contracts;
- communicate between Flutter and Kotlin through a typed platform API rather than UI code calling raw native methods;
- call the existing `face-biometric-service` contracts without introducing a mobile-specific backend;
- keep `face-client-simulator` as an independent Windows/browser QA reference;
- preserve a clear security boundary: client-side biometric output is evidence, not trusted authorization.

## 2. Component architecture

```
Flutter product UI
    |
    | typed Platform API / MethodChannel
    v
Kotlin Android biometric engine
    |
    +--> CameraX capture
    |
    +--> frame preprocessing / face alignment
    |
    +--> YuNet face detection
    |
    +--> MiniFASNetV2 + temporal liveness
    |
    +--> MobileFaceNet / w600k_mbf
    |
    +--> capture/session orchestration
    |
    v
REST client
    |
    v
face-biometric-service :8080 (default backend service port)
    |
    +--> verification policy
    +--> server-side verification
    +--> canonical verification response
```

The exact backend URL is runtime configuration; the diagram's port is illustrative of the existing service default and must not be hard-coded into production code.

## 3. Responsibility boundaries

### Flutter

Flutter owns:

- navigation and screens;
- customer/demo presentation;
- state presentation;
- method selection UI;
- reference/customer selection UI where applicable;
- progress, guidance and result presentation;
- settings and diagnostics presentation;
- user-safe error messages.

Flutter must not own:

- CameraX;
- camera frame acquisition;
- native ONNX inference;
- raw camera lifecycle;
- liveness model execution;
- native biometric preprocessing.

### Kotlin Android

Kotlin owns:

- CameraX lifecycle and frame acquisition;
- camera permissions integration;
- frame-rate/backpressure policy;
- native image conversion;
- face detection pipeline;
- quality/readiness calculations;
- liveness pipeline;
- MobileFaceNet inference;
- model loading/version validation;
- capture-session orchestration;
- native performance/resource management.

Kotlin exposes a stable, typed API to Flutter. Internal ML implementation details must not leak into Flutter widgets.

### Backend

`face-biometric-service` remains authoritative for:

- server-side verification;
- reference compatibility;
- model/version compatibility;
- policy enforcement;
- final verification status;
- production security controls.

The mobile client must not bypass the backend policy.

### Simulator

`face-client-simulator` remains a separate Windows/browser client for deterministic QA and backend troubleshooting. The Android app must not be implemented by wrapping, embedding or modifying the simulator.

## 4. Flutter ↔ Kotlin API boundary

The mobile module will use a versioned MethodChannel-based platform boundary.

Initial foundation API:

- channel namespace: `com.isc.face_mobile_demo/platform`
- health/capability query: `platform.health`

The API must evolve as typed Dart models and Kotlin DTO-like maps/objects. Raw MethodChannel calls should remain isolated in the platform adapter layer.

Planned native capabilities include:

- platform health;
- camera capability and permission state;
- capture lifecycle;
- face/quality guidance events;
- liveness state;
- embedding/capture results;
- verification-session lifecycle;
- diagnostics.

No UI widget should invoke MethodChannel methods directly.

## 5. Verification method mapping

The backend currently exposes these relevant verification contracts:

| Mobile method | Backend contract | Mobile responsibility |
|---|---|---|
| Full Clip | `POST /api/v1/biometric/video-verification/verify-clip` | Capture a bounded clip and submit according to backend contract |
| Server Live Stream | live-stream session APIs; final result at `POST /api/v1/live-stream/sessions/{sessionId}/complete` | Stream frames/session events and present server feedback |
| Client Embedding | `POST /api/v1/biometric/verify`; compatibility endpoint `POST /api/v1/biometric/verify-embedding` | Produce contract-compatible embedding and submit model metadata |
| Hybrid Best Frame | `POST /api/v1/biometric/hybrid-verification/verify` | Select a valid best frame and submit method-specific payload |
| Hybrid Multi Frame | `POST /api/v1/biometric/hybrid-multi-frame-verification/verify` | Collect valid frames and submit the multi-frame payload |
| Adaptive / policy-selected | resolved by backend policy support | Mobile asks/receives the authorized method; client does not invent policy |

The mobile implementation must consume the canonical verification response envelope instead of creating a different response model per method.

## 6. Backend response contract

The mobile client will parse the common verification envelope first and then interpret:

- overall status/result;
- match/no-match/indeterminate semantics;
- method;
- reason/error code;
- confidence/score fields when present and contractually meaningful;
- diagnostics only when explicitly exposed by the backend contract.

Backend error codes are data, not UI text. Flutter maps them to user-safe messages while retaining structured diagnostics for developer/demo mode.

## 7. Model contracts

### Face detection

- Model: YuNet `face_detection_yunet_2023mar.onnx`
- Execution: Android native ONNX Runtime Mobile
- Owner: Kotlin biometric engine

### Client recognition

- Model: `w600k_mbf.onnx`
- Profile: `mobilefacenet-512` / `w600k-mbf`
- Dimension: 512
- Input: 112x112
- Color order: RGB
- Normalization: approximately `(pixel - 127.5) / 128.0`
- Output: L2-normalized 512-D embedding

This profile is not interchangeable with the server ArcFace profile.

### Server recognition

- Model: `w600k_r50.onnx`
- Profile: `arcface-512` / `w600k-r50`
- Used by server-side video/full-clip and related server recognition flows.

### Anti-spoofing

- Model: `2.7_80x80_MiniFASNetV2.onnx`
- Used for client-side liveness stages where the phase requires it.
- Temporal movement/liveness remains a separate orchestration concern.

### Model asset rule

Model files are versioned assets, not arbitrary downloadable runtime inputs. The mobile application must verify expected model identity/version and must not silently substitute a different model or preprocessing contract.

## 8. Verification lifecycle

All methods should eventually fit a common lifecycle:

```
idle
  -> preparing
  -> permission/capability check
  -> capturing
  -> face detected
  -> quality/readiness
  -> liveness (when required)
  -> method-specific capture/encoding
  -> backend verification
  -> result
  -> cleanup
```

Failure/recovery must be explicit:

```
any active state -> recoverable error -> retry/reset
                                  \-> terminal configuration error
```

The common lifecycle is an extension point for M12 rather than an instruction to implement all methods early.

## 9. Configuration

Configuration is split into:

### Build/runtime environment

- environment name;
- backend base URL;
- demo/customer mode;
- logging/diagnostics level.

Flutter reads public runtime configuration and passes only required configuration across the platform boundary.

### Native biometric configuration

- camera constraints;
- model identifiers/versions;
- thresholds;
- frame sampling;
- liveness parameters;
- performance limits.

Security-sensitive production controls must be server-authoritative. Client configuration must never be treated as authorization.

## 10. Logging and diagnostics

Use structured, non-biometric diagnostics.

Allowed examples:

- phase/state transitions;
- session/correlation identifiers;
- model identifier/version;
- timing metrics;
- frame counts;
- backend HTTP status;
- structured error codes.

Do not log:

- raw face images;
- embeddings;
- biometric templates;
- credentials;
- access tokens;
- personal identity data.

Demo diagnostics may expose technical state but must not weaken production security.

## 11. Security boundary

Client Embedding is explicitly **untrusted evidence**.

Production security extension points must support:

- authenticated biometric sessions;
- server-issued challenge/nonce;
- replay prevention;
- binding evidence to the active session;
- device/app identity;
- app/device attestation where required;
- secure transport;
- server-authorized model/version policy;
- server-side validation and enforcement.

The mobile app must never be the sole authority for a successful identity decision.

## 12. Package structure

The target structure is:

```
face_mobile_demo/
  lib/
    app.dart
    main.dart
    core/
      config/
      platform/
      biometric/
      network/
      models/
      diagnostics/
    features/
      verification/
      settings/
      help/
      demo/
  android/
    app/src/main/kotlin/com/isc/face_mobile_demo/
      MainActivity.kt
      platform/
      biometric/
      camera/
      ml/
      liveness/
      network/
  test/
  android/
  prompts/
  docs/
```

M01 establishes only the minimum foundation. Later phases add packages without collapsing Flutter UI and native biometric infrastructure into one layer.

## 13. Dependency/version policy

Dependencies must be pinned or constrained deliberately and upgraded through explicit review.

Core decisions:

- Flutter stable channel;
- Kotlin native Android;
- CameraX for camera;
- ONNX Runtime Mobile for inference;
- no Java Android implementation;
- no dependency on the Windows simulator runtime;
- no hidden cloud biometric dependency;
- model versions must be explicit.

M03–M06 must finalize the exact CameraX/ONNX Runtime/model artifact versions after validating Android compatibility.

## 14. Android baseline

The project targets modern Android devices compatible with the selected Flutter/CameraX/ONNX Runtime Mobile stack.

The exact `minSdk`, ABI matrix and supported-device list are implementation/package decisions finalized in M18 after real-device compatibility testing. M00 therefore defines the boundary without prematurely freezing a value that has not yet been validated against the native ML stack.

## 15. CI and quality gates

Every mobile phase must keep these gates:

```
flutter pub get
flutter analyze
flutter test
flutter build apk --debug
```

Native Android tests/build checks are added when native functionality appears.

M19 expands this into the complete device/emulator test matrix and CI pipeline. M00 does not pretend emulator execution is a substitute for later camera/ML hardware validation.

## 16. Data flow by verification family

### Client-side evidence

```
CameraX -> YuNet -> quality/liveness -> MobileFaceNet
        -> 512-D normalized embedding
        -> authenticated/session-bound REST request
        -> backend verification
        -> canonical result
```

### Server-side video

```
CameraX -> bounded frames/clip -> REST
        -> face-biometric-service
        -> server detection/liveness/recognition
        -> canonical result
```

### Hybrid

```
CameraX -> client quality/liveness
        -> best frame(s)
        -> method-specific REST request
        -> server re-validation
        -> canonical result
```

## 17. Phase dependency map

| Phase | Depends on M00 decision |
|---|---|
| M01 Foundation | project/package/API boundary |
| M02 UI/UX | Flutter responsibility boundary |
| M03 Camera | CameraX ownership and frame boundary |
| M04 Detection | YuNet native ML boundary |
| M05 Quality | native capture pipeline |
| M06 Liveness | native anti-spoof + temporal orchestration |
| M07 Full Clip | REST/backend contract mapping |
| M08 Server Live Stream | live session contract |
| M09 Client Embedding | MobileFaceNet contract |
| M10 Hybrid Best Frame | common capture + hybrid backend contract |
| M11 Hybrid Multi Frame | common capture + multi-frame contract |
| M12 Method Selector | common lifecycle and method adapters |
| M13 Results | canonical response envelope |
| M14 Demo Mode | presentation/config separation |
| M15 Failure Recovery | common lifecycle |
| M16 Security | trust boundary and session hooks |
| M17 Performance | native pipeline ownership |
| M18 Compatibility | Android/model dependency policy |
| M19 Testing/CI | quality gates |
| M20 Release | all preceding contracts |

## 18. Explicit non-goals for M00

M00 does **not** implement:

- camera access;
- CameraX;
- YuNet inference;
- liveness inference;
- MobileFaceNet inference;
- REST verification calls;
- authentication;
- attestation;
- production biometric storage;
- customer identity data;
- complete UI;
- verification method execution.

Those belong to later phases.

## 19. Architecture acceptance

M00 is complete when:

1. the architecture and responsibility boundaries are documented;
2. backend verification contracts are mapped;
3. model contracts are explicit;
4. the security boundary is explicit;
5. dependency and Android baseline policy is explicit;
6. package structure and Flutter/Kotlin boundary are defined;
7. all later phases have documented dependencies;
8. the M00 audit/report is committed;
9. no later-phase implementation is incorrectly attributed to M00.
