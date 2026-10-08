# M01 Foundation — Phase Report

## Audit status

**IMPLEMENTED ON MOBILE BRANCH — MANUAL ANDROID VERIFICATION PENDING**

The M01 implementation is now present on branch `feat/mobile-m01-foundation-merge`, based on:

`0e2ef9d06a450f64f8113bca9c0e663b5cc8267c`

The implementation replaces the generated Flutter counter application with the M01 foundation. It must still pass the manual Android verification gate before M01 is marked ACCEPTED.

## Canonical prompt

`face_mobile_demo/prompts/01-MOBILE-FOUNDATION.md`

## Implemented

### Flutter foundation
- Replaced the generated counter application with a minimal application shell.
- Added `lib/app.dart`.
- Added `lib/core/config`, `lib/core/platform`, and `lib/core/biometric` boundaries.
- Added compile-time environment configuration through `--dart-define`.

### Flutter ↔ Kotlin bridge
- Added MethodChannel:
  `com.isc.face_mobile_demo/platform`
- Added typed Dart `PlatformApi`.
- Added typed `PlatformHealth`.
- Added native `getHealth` implementation.
- Native health response includes API name, bridge version, status, platform and engine state.

### Biometric engine boundary
- Added `BiometricEngine` interface.
- Added `PlaceholderBiometricEngine`.
- No camera or ML implementation was introduced in M01.

### Tests
- Replaced the generated counter test with an M01 widget test.
- The test verifies the typed platform-health presentation through the platform API boundary.

### Documentation
- Replaced the generated README with M01 setup, run, configuration and platform-contract documentation.
- Mobile roadmap and phase reports live under `face_mobile_demo/docs/`.

## Acceptance criteria

| Criterion | Status |
|---|---|
| Debug Android application builds/launches | IMPLEMENTED; manual verification required |
| Flutter calls Kotlin | IMPLEMENTED |
| Typed health/status response | IMPLEMENTED |
| Placeholder biometric engine | IMPLEMENTED |
| Clean-machine setup documented | IMPLEMENTED |
| Automated test baseline | IMPLEMENTED |
| No camera/ML in M01 | SATISFIED |
| Manual Android verification | **PENDING** |
| M01 accepted | **NO — pending manual gate** |

## Manual verification gate

From:

`face_mobile_demo/`

run:

```powershell
flutter pub get
flutter analyze
flutter test
flutter devices
flutter run -d <android-device-id>
```

The Android application must show:

- `M01 Foundation`
- `Platform bridge ready`
- API: `platform.health`
- Bridge: `1.0`
- Platform: `Android`
- Engine: `PLACEHOLDER`

M01 is fully accepted only after these checks pass on the Android emulator/device.

## Next step

Do not start M02 yet.

First complete the manual M01 verification gate. After successful verification, the report should be updated to **ACCEPTED** and only then should M02 begin.
