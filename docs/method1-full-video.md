# Method 1 — Full video clip to server

Method 1 is the only video verification flow enabled in this phase. The simulator records a 3–5 second browser clip and uploads the complete clip to `POST /api/v1/biometric/method-1/verify-clip` through the simulator proxy.

## Server pipeline

1. Validate upload size and decode the clip.
2. Sample frames at `biometric.method1.sample-fps`.
3. Run YuNet detection and require exactly one face.
4. Reject undersized faces and discard frames below the configured quality score.
5. Run MiniFASNetV2 liveness on candidate frames.
6. Sort candidates by quality and keep `recognition-frames` best frames.
7. Align each selected face and generate a normalized 512-D ArcFace/w600k-r50 embedding.
8. Compare every selected embedding with the reference using the existing `FaceMatcher` (cosine).
9. Aggregate scores using MEAN, MEDIAN or MAX and compare with the existing configurable biometric threshold.

## API

`multipart/form-data`: `referenceId`, `clip`, optional `requestId`.

Response result: `MATCH`, `NO_MATCH`, `INCONCLUSIVE`, `INVALID_REQUEST`, or `PROCESSING_ERROR`.

Diagnostic codes are intentionally coarse: `NO_FACE`, `MULTIPLE_FACES`, `LOW_LIGHT`, `BLUR`, `FACE_TOO_SMALL`, `BAD_POSE`, `LIVENESS_FAILED`, `INVALID_VIDEO`, `VIDEO_TOO_LONG`, `VIDEO_TOO_LARGE`, `MODEL_ERROR`, `REFERENCE_NOT_FOUND`.

## Configuration

See `face-biometric-service/src/main/resources/application.yml`. Model files are configured by path and are not committed to the repository.

## Manual test

1. Put YuNet, MiniFASNetV2 and `w600k_r50.onnx` at the configured paths.
2. Start `face-biometric-service` on port 8090.
3. Start `face-client-simulator`.
4. Open the simulator UI and confirm the badge says `METHOD 1 · FULL VIDEO → SERVER`.
5. Select a webcam, choose 3–5 seconds, capture and review the clip.
6. Enter a reference ID that exists in the configured reference repository.
7. Send the full clip and inspect the returned status, reason codes, decoded frame count, recognition frame count and processing latency.
8. Repeat with an invalid file, an over-size file, a clip longer than five seconds, no face, two faces and a low-quality capture.

## Performance

The API reports server processing time and frame counts. The simulator reports the captured upload size and client round-trip time. No biometric accuracy claim is made by this project.

## Limitations

Browser `MediaRecorder` commonly produces WebM. Video decoding therefore depends on the OpenCV native build's available video codecs. Model paths must be provisioned separately. The configured threshold is an application parameter, not a claim of banking-grade calibration. Methods 2–5, policy evaluation and adaptive fallback are deliberately outside this phase.
