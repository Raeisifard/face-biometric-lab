# M01 Foundation — Phase Report

## Audit status

**IMPLEMENTED — MANUAL ANDROID VERIFICATION PENDING**

The M01 implementation is now on the merge branch and replaces the generated Flutter counter application with the M01 foundation.

## Canonical prompt

`face_mobile_demo/prompts/01-MOBILE-FOUNDATION.md`

## Implemented

- Flutter application shell replacing the generated counter app.
- Flutter core boundaries for configuration, platform bridge and biometric engine.
- Typed `PlatformApi` and `PlatformHealth`.
- Kotlin `MethodChannel` at `com.isc.face_mobile_demo/platform`.
- Native `getHealth` response with API name, bridge version, status, platform and engine state.
- `PlaceholderBiometricEngine` boundary.
- M01 widget test replacing the generated counter test.
- M01 README and configuration/run documentation.
- No camera or ML implementation in M01.

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

From `face_mobile_demo/`:

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

M01 becomes **ACCEPTED** only after this Android verification passes.

## Next step

Do not start M02 until the manual M01 gate is confirmed.
