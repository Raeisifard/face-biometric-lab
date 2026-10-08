# M01 Foundation — Phase Report

## Audit status

**IMPLEMENTED ON MOBILE BRANCH — NOT YET MERGED TO MASTER — MANUAL VERIFICATION PENDING**

The M01 implementation exists in commit:

`0e2ef9d06a450f64f8113bca9c0e663b5cc8267c`

However, that commit is not currently an ancestor of `master`. Therefore M01 must **not** be considered merged/accepted on `master`.

This report is intentionally recorded under the canonical mobile documentation path so the phase history is visible from `master`.

## Canonical prompt

`face_mobile_demo/prompts/01-MOBILE-FOUNDATION.md`

## Implemented in M01

### Flutter foundation
- Replaced the generated counter application with a minimal application shell.
- Added `lib/app.dart`.
- Added `lib/core/config`, `lib/core/platform`, and `lib/core/biometric` boundaries.
- Added compile-time environment configuration through `--dart-define`.

### Flutter ↔ Kotlin bridge
- Added MethodChannel:
  `com.isc.face_mobile_demo/platform`
- Added typed Dart `PlatformApi`.
- Added typed `PlatformHealth` model.
- Added native `getHealth` implementation.
- Native health response includes API name, bridge version, status, platform and engine state.

### Biometric engine boundary
- Added `BiometricEngine` interface.
- Added `PlaceholderBiometricEngine`.
- No camera or ML implementation was introduced in M01.

### Tests
- Replaced the generated counter test with an M01 widget test.
- Test verifies the typed platform-health presentation through the platform API boundary.

### Documentation
- Replaced the generated README with M01 setup/run/configuration/platform-contract documentation.
- Mobile phase reports and roadmap use `face_mobile_demo/docs/`.

## Acceptance criteria

| Criterion | Status |
|---|---|
| Flutter project launches on Android | IMPLEMENTED; manual verification required |
| Flutter calls Kotlin | IMPLEMENTED |
| Typed health/status response | IMPLEMENTED |
| Placeholder biometric engine | IMPLEMENTED |
| Clean-machine setup documented | IMPLEMENTED |
| Automated test baseline | IMPLEMENTED |
| No camera/ML in M01 | SATISFIED |
| M01 implementation merged to master | **NO** |
| Manual Android verification | **PENDING** |

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

- `Platform bridge ready`
- API: `platform.health`
- Bridge: `1.0`
- Platform: `Android`
- Engine: `PLACEHOLDER`

M01 is fully accepted only after these checks pass and the M01 implementation commit is merged to `master`.

## Important repository finding

The M01 implementation commit was created after the earlier M01 assessment, but it was never merged into `master`. The current `master` therefore contains the M00 correction and the earlier mobile baseline, but not the complete M01 implementation.

This explains why the M01 report/implementation was not visible from the current `master` tree.

## Next step

Do **not** start M02 yet.

First:
1. merge the existing M01 implementation into `master`;
2. run the manual M01 verification gate;
3. update this report to **ACCEPTED** only after the verification passes.

