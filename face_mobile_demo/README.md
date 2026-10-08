# Face Mobile Demo

Flutter + Kotlin Android client for the Face Biometric Lab.

## Current phase

**M02 — UI/UX**

M02 establishes the product-facing Flutter shell, navigation, design system, verification state flow, method/reference selection, Customer/Developer modes, and Help/About content.

M02 intentionally contains no CameraX, face detection, liveness, ONNX, embedding generation, or backend execution.

## Run

From face_mobile_demo/:

flutter pub get
flutter analyze
flutter test
flutter devices
flutter run -d <android-device-id>

## M02 flow

- Verify — Ready -> Capture placeholder -> Processing -> Match / No Match.
- Method — Full Clip, Server Live Stream, Client Embedding, Hybrid Best Frame, Hybrid Multi Frame.
- Reference — deterministic demo reference selections.
- Settings — Customer or Developer mode.
- Help — verification, security-boundary and phase-scope guidance.

Processing/result controls are deterministic demo controls only. They do not call the server.

## Configuration

flutter run -d <android-device-id> --dart-define=APP_ENVIRONMENT=dev --dart-define=FACE_SERVICE_BASE_URL=http://10.0.2.2:8090 --dart-define=APP_DEMO_MODE=true

## Architecture boundary

Flutter owns presentation and orchestration. Android/Kotlin owns native camera and biometric infrastructure introduced in later phases. M02 keeps that boundary intact.

## Verification

flutter analyze
flutter test

No real biometric data or secrets belong in source control.
