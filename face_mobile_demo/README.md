Flutter + Kotlin Android foundation for the Face Biometric Lab mobile client.

## Current phase

**M01 — Foundation**

M01 establishes the Flutter presentation layer, Android/Kotlin native boundary, typed Dart platform API, native platform health/status response, placeholder biometric-engine abstraction, build/lint/test baseline, and compile-time environment configuration.

There is intentionally no camera or ML implementation yet.

## Prerequisites

- Flutter stable compatible with the SDK constraint in `pubspec.yaml`.
- Android Studio with Android SDK and an Android emulator or physical Android device.
- Android SDK licenses accepted.

Visual Studio is **not** required for the Android target.

## Run

From `face_mobile_demo/`:

```powershell
flutter pub get
flutter analyze
flutter test
flutter devices
flutter run -d <android-device-id>
```

## Configuration

The mobile app uses compile-time Dart defines:

```powershell
flutter run -d <android-device-id> --dart-define=APP_ENVIRONMENT=dev --dart-define=FACE_SERVICE_BASE_URL=http://10.0.2.2:8090 --dart-define=APP_DEMO_MODE=true
```

The defaults are development-only values and can be overridden per environment.

## M01 platform contract

- MethodChannel: `com.isc.face_mobile_demo/platform`
- Method: `getHealth`
- Response: `apiName`, `bridgeVersion`, `status`, `platform`, `engineState`
- Bridge version: `1.0`
- Engine state: `PLACEHOLDER`

The Dart side maps this response into the typed `PlatformHealth` model.

## Architecture boundary

Flutter owns presentation and orchestration. Android/Kotlin owns native camera and biometric infrastructure that will be introduced in later phases. M01 exposes only the health/status bridge and a placeholder engine abstraction.

## Verification

A successful M01 run shows **Platform bridge ready**, `platform.health`, bridge version `1.0`, Android platform, and `PLACEHOLDER` engine state.

No real biometric data or secrets belong in source control.
