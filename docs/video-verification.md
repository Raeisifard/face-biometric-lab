# Server Video Verification

The project supports two server-selected capture methods. `FULL_CLIP` records a 3–5 second browser clip and uploads the complete clip to `POST /api/v1/biometric/video-verification/verify-clip`. `LIVE_STREAM` creates a session and uploads sampled JPEG frames incrementally through `/api/v1/live-stream/sessions/{sessionId}/frames`.

Set the server decision with `BIOMETRIC_CAPTURE_METHOD=LIVE_STREAM` or `BIOMETRIC_CAPTURE_METHOD=FULL_CLIP`. The default is `LIVE_STREAM`. The simulator reads `GET /api/v1/biometric/capture-method` through its proxy and only enables the selected flow.

## Server pipeline

1. Validate upload size and decode the clip.
2. Sample frames at `biometric.video-verification.sample-fps`.
3. Run YuNet detection and require exactly one face.
4. Reject undersized faces and discard frames below the configured quality score.
5. Run MiniFASNetV2 liveness on candidate frames.
6. Sort candidates by quality and keep `recognition-frames` best frames.
7. Align each selected face and generate a normalized 512-D ArcFace/w600k-r50 embedding.
8. Compare every selected embedding with the reference using the existing `FaceMatcher` (cosine).
9. Aggregate scores using MEAN, MEDIAN or MAX and compare with the existing configurable biometric threshold.

## Observability

The server logs the request ID through the full pipeline. INFO logs cover request/upload metadata, media metadata, validation failures, candidate counts, reference loading, score aggregation and final timing. DEBUG logs cover individual frame detection, quality, liveness and cosine scores. Logs never contain face images or full embedding vectors.

The decoder logs reported FPS, reported frame count, calculated duration, decoded frame count and sampling step. This is intentionally exposed because browser `MediaRecorder` metadata can be the source of an apparent duration/`INVALID_VIDEO` problem.

## API

`multipart/form-data`: `referenceId`, `clip`, optional `requestId`.

Response result: `MATCH`, `NO_MATCH`, `INCONCLUSIVE`, `INVALID_REQUEST`, or `PROCESSING_ERROR`.

Diagnostic codes include `NO_FACE`, `MULTIPLE_FACES`, `FACE_TOO_SMALL`, `LIVENESS_FAILED`, `INVALID_VIDEO`, `VIDEO_TOO_LONG`, `VIDEO_TOO_LARGE`, `MODEL_ERROR`, and `REFERENCE_NOT_FOUND`.

## Configuration

See `face-biometric-service/src/main/resources/application.yml` under `biometric.video-verification`. Model files are configured by path and are not committed to the repository.

## Manual test

1. Put YuNet, MiniFASNetV2 and `w600k_r50.onnx` at the configured paths.
2. Start `face-biometric-service` on port 8090.
3. Start `face-client-simulator` on port 8091.
4. Open the simulator UI and confirm the badge says `SERVER VIDEO VERIFICATION`.
5. Select a webcam, choose 3–5 seconds, capture and review the clip.
6. Enter a reference ID that exists in the configured reference repository.
7. Send the full clip and inspect the returned status, reason codes, decoded frame count, recognition frame count and processing latency.
8. If the result is `INVALID_VIDEO`, inspect the service logs for FPS, reported frame count and calculated duration before changing biometric thresholds or recognition code.

For the live-stream method, open the same simulator page, click **Start camera**, enter the reference ID in the live panel, choose an upload FPS, and click **Start live session**. The browser samples the preview into JPEG frames and sends one frame at a time. The panel displays the session state, machine-readable server feedback, frame count, uploaded bytes and request latency. Click **Stop live session** to send the completion request. Frames are bound to the server session and are rejected after expiration, completion or a capture-mode mismatch.

## Limitations

Browser `MediaRecorder` commonly produces WebM. Video decoding therefore depends on the OpenCV native build's available video codecs. Model paths must be provisioned separately. The configured threshold is an application parameter, not a claim of banking-grade calibration. Other verification modes, policy evaluation and adaptive fallback are deliberately outside this phase.
