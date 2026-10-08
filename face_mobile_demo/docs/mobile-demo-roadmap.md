# Android Mobile Demo — Master Roadmap

## Goal
Build a product-like Android demo client for face-biometric-lab. It captures camera input, performs required client-side biometric stages, calls face-biometric-service, and presents Match/No Match for customer demonstrations.

## Canonical mobile documentation
All mobile phase prompts are under `face_mobile_demo/prompts/`. All mobile phase reports and the mobile roadmap are under `face_mobile_demo/docs/`.

The root `prompts/` directory contains legacy/general project prompts and is not the source of truth for mobile phases.

## Technology decision
- Flutter for UI, navigation and demo presentation.
- Kotlin native Android for CameraX, real-time frames and ONNX Runtime Mobile.
- REST/JSON to face-biometric-service.
- Do not wrap face-client-simulator; keep it as Windows/browser QA reference.

## Architecture
Flutter UI -> typed MethodChannel/platform API -> Kotlin biometric engine -> CameraX/ONNX models -> REST -> face-biometric-service.

## Verification methods
1. Full Clip
2. Server Live Stream
3. Client Embedding
4. Hybrid Best Frame
5. Hybrid Multi Frame
6. Adaptive/Policy-selected mode when backend supports it

## Execution order
M00 Architecture
M01 Foundation
M02 UI/UX
M03 Camera
M04 Face Detection
M05 Quality & Guidance
M06 Liveness
M07 Full Clip
M08 Server Live Stream
M09 Client Embedding
M10 Hybrid Best Frame
M11 Hybrid Multi Frame
M12 Method Selector
M13 Results & Diagnostics
M14 Demo Mode
M15 Failure & Recovery
M16 Security
M17 Performance
M18 Compatibility & Packaging
M19 Testing & CI
M20 Release

## Model contract baseline
Current client embedding contract: `w600k_mbf`, 512-D, 112x112, RGB, `(pixel - 127.5) / 128.0`, L2-normalized. No silent preprocessing/model substitution.

## Security boundary
Client Embedding is untrusted evidence. Keep extension points for authenticated sessions, nonce binding, replay prevention, device/app identity, attestation, secure transport, model/version policy and server-side enforcement.

## Repository target
The mobile application lives at `face_mobile_demo/` at repository root. Existing backend and simulator remain independently runnable.

## Phase gate
Every phase must be implemented, tested, documented and committed before the next phase begins.
