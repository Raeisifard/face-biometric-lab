# M04 — Face Detection Report

## Status

Implementation in progress on `feat/mobile-m04-face-detection`. CameraX preview is intentionally independent from YuNet initialization so detector/model failures cannot remove the live preview.

## Scope

M04 integrates the canonical YuNet `face_detection_yunet_2023mar.onnx` model through native Android ONNX Runtime and keeps inference outside Flutter.

Delivered in this phase:

- ONNX Runtime Android dependency pinned to 1.20.0.
- YuNet model loading from Android assets.
- Build-time copy of the repository-level YuNet model when present locally.
- YUV_420_888 camera frame conversion to BGR NCHW input.
- 320x320 YuNet inference.
- YuNet output decoding for strides 8/16/32.
- confidence threshold 0.80.
- IoU NMS threshold 0.30 with top-K 5000.
- bounding-box and five-landmark coordinate conversion back to source image coordinates.
- one-face state classification:
  - NO_FACE
  - SINGLE_FACE
  - MULTIPLE_FACES
- compact typed detection events over the existing camera EventChannel.
- asynchronous/lazy YuNet initialization so CameraX preview remains available when the model or detector is unavailable.
- detector initialization errors are reported as `FACE_DETECTOR_UNAVAILABLE` events instead of failing `PlatformView` creation.
- Flutter detection status presentation for device verification.
- Dart coverage for detection event parsing and malformed metadata.

## Explicit non-goals

M04 does not implement:

- face quality scoring;
- liveness or anti-spoofing;
- face alignment for recognition;
- MobileFaceNet embeddings;
- backend requests;
- identity matching;
- biometric authorization.

## Model asset

The binary model is intentionally not committed. The canonical repository model is:

`models/detector/face_detection_yunet_2023mar.onnx`

The Android build copies it into:

`android/app/src/main/assets/models/`

when the source model exists locally.

## Coordinate contract

Detector coordinates are reported in the rotated CameraX analysis-image coordinate space. The event also includes the source image width and height so later phases can transform the coordinates into the preview/guide coordinate space without coupling Flutter to YuNet internals.

## Acceptance gate

M04 is accepted only after:

1. `flutter analyze` passes.
2. `flutter test` passes.
3. `flutter build apk --debug` passes with the YuNet model available.
4. On an Android device/emulator:
   - no-face produces `NO_FACE`;
   - one visible face produces `SINGLE_FACE`;
   - two or more visible faces produce `MULTIPLE_FACES`;
   - detection remains stable while the device rotates;
   - no camera frame backlog or resource-retention issue is observed.

The next phase must not start until these checks pass.
