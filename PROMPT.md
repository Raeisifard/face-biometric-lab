# AI coding prompt for this project

Create and maintain a two-module Java 21 / Spring Boot 3.5.x Maven project named `face-biometric-lab`.

## Module A — `face-client-simulator`

Build a Windows 11 friendly browser-based mobile client simulator. Use Spring Boot as the core runtime and serve a modular HTML/CSS/JavaScript UI. Use the browser `MediaDevices.getUserMedia()` API so the operator can choose any `videoinput` camera available to Chrome/Edge. Show the live stream in a video frame. Sample the stream at a configurable low FPS and send JPEG frames to Spring Boot. Optionally support local video-file replay for deterministic tests.

The simulator pipeline is:

`camera stream -> YuNet detector -> five-point landmarks -> MiniFASNetV2 anti-spoofing -> temporal movement liveness -> ArcFace/InsightFace w600k_r50 -> normalized 512-D embedding`.

The liveness result must be session-scoped and require both a configurable average anti-spoof score and temporal motion. Keep liveness separate from recognition and expose replaceable `LivenessModel`/`LivenessEngine` interfaces.

Use ONNX Runtime Java for ArcFace and MiniFASNet inference. Use OpenCV Java for YuNet and image alignment. The ArcFace implementation must validate the ONNX output dimension is exactly 512 and normalize the vector.

Provide REST endpoints for session creation, sampled-frame analysis, embedding generation, enrollment, and verification. For enrollment/verification the simulator must call Module B using the vector payload, exactly as a future Flutter client would. Never mix CID/DID, OTP, user assurance or token issuance into the biometric model code.

## Module B — `face-biometric-service`

Build an independent REST service that does not need camera access. It accepts a normalized 512-D probe embedding with `userId`, `modelId`, and `modelVersion`. It retrieves the registered reference embedding and computes cosine similarity. Reject wrong dimensions, zero vectors, model/version mismatches and non-normalized vectors when required.

Implement interchangeable repositories:

- `memory` for local bring-up
- `oracle` for the banking system of record
- `mongo` for experiments

For Oracle use a `FACE_BIOMETRIC_PROFILE` table with a BLOB containing 512 float32 values. Include Flyway migration and do not assume the Oracle SYSTEM schema is an application schema.

## Model abstraction

Keep interfaces for `FaceDetector`, `FaceEmbeddingModel`, `FaceAligner`, `LivenessDetector`, `FaceSimilarityCalculator`, and `FaceEmbeddingRepository`. The first baseline should work with YuNet + ArcFace/InsightFace w600k_r50 + MiniFASNetV2. New detectors, recognizers and liveness models must be addable without changing controllers.

## UI

Use modular static assets, no React/Vue/Angular/Node build. Provide camera selector, start/stop controls, live preview, detector bounding box overlay, liveness score, temporal-motion score, instruction, embedding metadata, enrollment and verification controls. Include help buttons/tooltips for technical concepts.

## Privacy

Never log raw face images or complete embeddings by default. Use IDs, dimensions, model/version, scores, decisions and timings.

## Deliverables

Provide complete Maven source, tests, Oracle schema/Flyway migration, model installation documentation, PowerShell setup scripts, API documentation, architecture docs, and a README explaining that public InsightFace pretrained models are currently research-only unless separately licensed for commercial use.
