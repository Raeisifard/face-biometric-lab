# M05 — MediaPipe BlazeFace Mobile Detector

This branch experiments with Google's MediaPipe Face Detector (BlazeFace short-range) in the Android camera pipeline. The existing M04 YuNet branch is unchanged.

## Branch

`feat/mobile-m05-mediapipe-face-detector`

[Open branch on GitHub](https://github.com/Raeisifard/face-biometric-lab/tree/feat/mobile-m05-mediapipe-face-detector)

## Model download

Official Google-hosted model:

- [Download the official BlazeFace short-range TFLite model](https://storage.googleapis.com/mediapipe-models/face_detector/blaze_face_short_range/float16/1/blaze_face_short_range.tflite)

The Android Gradle build downloads this model automatically to:

`face_mobile_demo/android/app/src/main/assets/models/face_detection_short_range.tflite`

To add it manually, download the file from the link above and save it at exactly that path. The Gradle task skips downloading when the file already exists. The model is intentionally not committed to Git.

## Implementation

- Uses `com.google.mediapipe:tasks-vision:0.10.21`.
- Uses MediaPipe `LIVE_STREAM` mode so inference is asynchronous.
- Only submits one frame at a time; frames are skipped while inference is in progress.
- Emits the same Flutter event shape as M04: `status`, `modelId`, `imageWidth`, `imageHeight`, `processingMs`, and face boxes/landmarks.
- Reports the model ID as `mediapipe-blazeface-short-range`.
- Uses a 0.65 minimum detection confidence and 0.3 suppression threshold as initial experimental values.

## Build and test

From `face_mobile_demo`:

```bash
flutter pub get
cd android
gradlew.bat :app:assembleDebug
```

The first Android build needs internet access to download the model and resolve the MediaPipe dependency. A physical Android device is recommended for latency measurements.

## Acceptance checks

1. Confirm the first build downloads the model and the APK builds successfully.
2. On the same phone and lighting, record `processingMs` for at least 30 detections.
3. Verify one face produces one box, no face produces zero boxes, and a second visible face produces two boxes.
4. Confirm the preview remains responsive and no inference backlog accumulates.
5. Compare latency and stability with M04 YuNet on the same device.

This is an experimental branch. Its source changes have not been built or exercised on a physical device in this environment; run the build and device checks above before treating it as validated.
