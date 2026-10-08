# M01 Foundation — Phase Report

## Status

**NOT COMPLETE — M02 must not start yet.**

The latest mobile commit `c5213c092f4e6ee2bc2fd5dff46bb9c24d68fa01` successfully launches the generated Flutter application on an Android Studio emulator, but it does not satisfy the M01 acceptance criteria.

## Baseline reviewed

- Commit: `c5213c092f4e6ee2bc2fd5dff46bb9c24d68fa01`
- Commit message: `First face_mobile_demo App run`
- Mobile project: `face_mobile_demo/`
- Mobile prompts: `face_mobile_demo/prompts/`

## M01 requirements checked

| Requirement | Status | Finding |
|---|---|---|
| Flutter project exists and launches on Android | PASS | The Flutter project is present and the first Android emulator run was successful. |
| Android/Kotlin native bridge | FAIL | `MainActivity.kt` is only the default `FlutterActivity`; no platform bridge is implemented. |
| Typed platform API | FAIL | No typed Flutter ↔ Kotlin health/status API exists. |
| Placeholder biometric engine | FAIL | No Kotlin biometric-engine abstraction/placeholder is present. |
| Clean package structure | FAIL | `lib/main.dart` is still the default Flutter counter application. |
| App identity/build configuration | PARTIAL | Android application ID exists, but the project remains essentially default generated configuration. |
| Environment configuration | FAIL | No mobile environment/configuration layer has been implemented. |
| Lint/format baseline | PASS | Flutter lint configuration exists. |
| Automated tests | FAIL | Only the generated default widget test is present; M01 bridge/contract tests are absent. |
| Clean-machine setup documentation | FAIL | README remains the generated Flutter README and does not document the mobile project setup/architecture. |

## Evidence

The current `face_mobile_demo/lib/main.dart` is the standard Flutter counter template.

The current Android entry point is:

`com.isc.face_mobile_demo.MainActivity : FlutterActivity`

with no MethodChannel/platform API or native biometric engine abstraction.

The current `pubspec.yaml` contains only the default Flutter/Cupertino dependencies and `flutter_lints`.

## Decision

M01 is **not accepted**.

The successful emulator launch is a valid M01 prerequisite/baseline, but it is not sufficient to close M01.

Therefore:

**M01 remains the active implementation phase. M02 must wait until M01 is fully implemented, tested, documented, committed, and manually verified on the Android emulator.**

## Documentation location

All mobile phase reports belong under:

`face_mobile_demo/docs/`

The mobile master roadmap is also located under:

`face_mobile_demo/docs/mobile-demo-roadmap.md`.

The repository-root `docs/mobile-demo-roadmap.md` is no longer the mobile roadmap location.
