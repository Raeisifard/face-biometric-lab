# M01 Foundation — Phase Report

## Status

**IMPLEMENTED — pending manual Android emulator verification.**

M01 has now been implemented against the canonical prompt at `face_mobile_demo/prompts/01-MOBILE-FOUNDATION.md`.

## Baseline

- Previous baseline: `2d09781c767ae841213a6ace5a15885087c117ea`
- Mobile project: `face_mobile_demo/`
- Mobile prompts: `face_mobile_demo/prompts/`

## Implemented

### Flutter foundation
- Replaced the generated counter application with a minimal production-oriented application shell.
- Added clear `lib/app.dart`, `lib/core/config`, `lib/core/platform`, and `lib/core/biometric` boundaries.
- Added compile-time environment configuration through `--dart-define`.

### Flutter ↔ Kotlin bridge
- Added MethodChannel `com.isc.face_mobile_demo/platform`.
- Added typed Dart `PlatformApi` and `PlatformHealth` models.
- Added native `getHealth` implementation.
- Native response includes API name, bridge version, status, platform and engine state.

### Biometric engine boundary
- Added `BiometricEngine` interface and `PlaceholderBiometricEngine`.
- No camera or ML implementation is introduced in M01.

### Tests
- Replaced the generated counter test with an M01 widget test.
- The test verifies the typed platform health presentation through an injected platform API.

### Documentation
- Replaced the generated README with M01 setup, run, configuration and platform-contract documentation.
- Mobile roadmap and phase reports are kept under `face_mobile_demo/docs/`.

## Acceptance criteria

| Criterion | Status |
|---|---|
| Debug Android application builds/launches | READY — previously verified on Android emulator |
| Flutter calls Kotlin | IMPLEMENTED |
| Typed health/status response | IMPLEMENTED |
| Clean-machine setup documented | IMPLEMENTED |
| Automated test baseline | IMPLEMENTED |
| No camera/ML in M01 | SATISFIED |

## Manual verification required

From `face_mobile_demo/`:

```powershell
flutter pub get
flutter analyze
flutter test
flutter devices
flutter run -d <android-device-id>
```

The running Android app must show:

- `Platform bridge ready`
- API: `platform.health`
- Bridge: `1.0`
- Platform: `Android`
- Engine: `PLACEHOLDER`

M01 is accepted only after these checks pass on the Android emulator/device.

## Next phase

M02 — UI/UX remains blocked until the manual M01 gate above is confirmed.
