# M03 CameraX Report

## Status

Implementation complete on branch `feat/mobile-m03-camera`.

## Scope delivered

- CameraX dependencies added to the Android app.
- Runtime `CAMERA` permission declared and requested.
- Front-facing camera selected.
- CameraX `Preview` rendered through an Android `PreviewView`.
- `ImageAnalysis` configured with:
  - target resolution 1280x720;
  - `STRATEGY_KEEP_ONLY_LATEST` bounded backpressure;
  - output image rotation enabled;
  - frame resources always closed.
- Rotation changes trigger a safe CameraX rebind.
- Android lifecycle owns the CameraX use cases through `bindToLifecycle`.
- PlatformView disposal unbinds use cases, clears the analyzer and shuts down the executor.
- A typed Flutter `CameraApi` exposes throttled frame metadata for later phases without transferring raw camera frames through Flutter.
- Camera permission recovery restarts the existing camera view rather than recreating the Activity.
- M03 verification flow now shows the real Android CameraX preview; non-Android widget-test environments retain a deterministic fallback message.

## Explicit non-goals

M03 does not implement:

- YuNet face detection;
- face quality scoring;
- liveness;
- ONNX Runtime;
- embedding generation;
- backend requests;
- biometric decision logic.

Those remain later phases.

## Testing

Dart unit coverage validates camera frame metadata parsing. Existing M02 widget tests remain deterministic because the camera preview has a non-Android test fallback.

Manual acceptance requires an Android physical device with camera permission granted. Verify:

1. Open Verify Identity and start verification.
2. Grant camera permission.
3. Confirm stable front-camera preview.
4. Rotate the device and confirm preview/analysis remain active.
5. Background/resume the app and confirm CameraX releases/rebinds through lifecycle.
6. Confirm no unbounded frame queue or retained analyzer is observed.

## Acceptance gate

M03 is ready for acceptance after the physical-device lifecycle/rotation checks above pass.

## References

CameraX use cases and lifecycle binding follow the Android CameraX architecture guidance: Preview and ImageAnalysis can be bound together to a lifecycle, and PreviewView is the recommended preview surface.
