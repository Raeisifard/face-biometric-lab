# Mobile Client Face Biometric Laboratory & Benchmark Plan

**Date:** 2026-10-14  
**Project:** `face-biometric-lab`  
**Scope:** `face_mobile_demo` / Flutter + Android client  
**Current reference branch:** `feat/mobile-m05-mediapipe-face-detector`  
**Status:** Planning baseline for the next implementation chat

## 1. Purpose

This document records the mobile/client-side decisions and implementation plan agreed for the next development phase.

The mobile application is intentionally a **demo/laboratory client for the server**, not the final production biometric client. Its primary purpose is to test server verification services using different acquisition and biometric execution modes, compare client-side versus server-side work, benchmark alternative models/runtimes/backends, and discover the best end-to-end flow and settings.

The application should therefore favor observability, experimentability, repeatability and clean separation of concerns over premature production hardening.

## 2. Primary goals

- Demonstrate server biometric capabilities.
- Compare multiple verification modes.
- Exercise client-side detection, liveness and embedding where useful.
- Measure mobile CPU/GPU/NPU acceleration where supported.
- Compare different detectors/runtimes.
- Provide reproducible benchmark runs on real Android devices.
- Make the client a laboratory for selecting the best flow, model and runtime settings.
- Keep Flutter UI independent from the underlying ML runtime.

## 3. Current M05 state

M05 uses a native Android MediaPipe Face Detector/BlazeFace path and CameraX. The detector is used in LIVE_STREAM mode and the implementation prevents more than one inference from being in flight at a time; frames arriving while inference is busy are skipped.

This is a useful real-time design because it prevents an unbounded inference backlog.

The current branch also exposes conceptual modes including:

- Full Clip
- Server Live Stream
- Client Embedding
- Hybrid Best Frame
- Hybrid Multi Frame

In the current M05 UI these modes are not all fully implemented yet; Client Embedding is a planned capability and must become a real execution path in later work.

## 4. Core architecture

The mobile application should evolve toward:

```text
Flutter UI
   |
   v
VerificationController
   |
   v
VerificationEngine
   |
   +--> ServerFullClip
   +--> ServerLiveStream
   +--> ClientEmbedding
   +--> HybridBestFrame
   +--> HybridMultiFrame
              |
              v
       MobileInference
              |
       +------+-------+----------------+
       |              |                |
 DetectionProvider  LivenessProvider  EmbeddingProvider
```

The Flutter layer should orchestrate UI/state and request execution, while heavy image/inference work should remain in Android native code as much as practical.

## 5. Camera architecture

Prefer a native CameraX pipeline for analysis:

```text
CameraX
  |
  +--> Preview -> Flutter/UI
  |
  +--> ImageAnalysis -> Native ML pipeline
                         |
                         v
                      result
                         |
                         v
                     Flutter UI
```

Avoid repeatedly copying full camera pixel buffers from native Android into Dart and then back into native code. Keep image data native through preprocessing and inference whenever practical.

Flutter should receive compact results such as bounding boxes, scores, state transitions, telemetry and final embeddings rather than unnecessary full-resolution frames.

## 6. Client Embedding mode

Client Embedding must be implemented as an actual laboratory mode rather than only a UI placeholder.

Target flow:

```text
Camera
  |
  v
Face Detection
  |
  v
Face Tracking / Stability
  |
  v
Quality Gate
  |
  v
Best Frame Selection
  |
  v
Face Alignment / Preprocessing
  |
  v
ArcFace
  |
  v
Embedding
  |
  v
Normalization
  |
  v
Server verification API
```

The client should not send the full video/clip in this mode. It should produce a biometric embedding locally and submit the embedding/evidence according to the server API contract.

Client-generated embeddings must be treated as **untrusted client evidence** by the server. The demo mode is for capability and benchmark experiments and must not imply that a client-created embedding is inherently trustworthy for production authentication.

## 7. Embedding contract

The client and server must explicitly agree on:

- model ID
- model version
- input dimensions
- color/channel order
- preprocessing
- normalization
- embedding dimension
- embedding numeric representation
- distance/similarity metric
- threshold/policy

For the current ArcFace direction, the expected embedding contract is a 512-dimensional normalized embedding where supported by the actual model configuration. The exact model metadata must always be read from the current repository/model configuration before implementation.

Do not allow a client to submit an embedding whose model/version/preprocessing metadata cannot be identified.

## 8. Detection, liveness and recognition separation

Do not couple detection directly to embedding generation.

Use a pipeline:

```text
Frame
 -> DetectionProvider
 -> FaceRegion
 -> FacePreprocessor
 -> EmbeddingProvider
 -> Embedding
```

This enables combinations such as:

```text
MediaPipe detection + ArcFace embedding
YuNet detection      + ArcFace embedding
```

without changing recognition logic.

Likewise liveness must be an independent provider.

## 9. Model responsibilities

### Detection

Current experimental detector: MediaPipe BlazeFace/Face Detector.

Previous/alternative detector: YuNet.

Detection should be optimized for continuous real-time operation. It does not need to run on every camera frame if tracking and frame sampling can maintain stable results.

### Liveness

Target model: MiniFASNetV2.

Liveness should normally run only after a face has been detected and is sufficiently stable/qualified. It should not blindly execute on every camera frame.

Where temporal evidence is useful, use multiple sampled frames for the liveness decision instead of increasing raw inference rate unnecessarily.

### Recognition

Target model: ArcFace.

Recognition is the most expensive operation in the verification sequence and should normally be executed only after detection, quality and liveness gates pass.

A practical target is one or a small number of high-quality ArcFace evaluations per verification attempt rather than running recognition at camera FPS.

## 10. Recommended real-time pipeline

```text
Camera: ~30 FPS
       |
       v
Smart frame sampling
       |
       v
Detection: configurable rate
       |
       v
Face tracking/stability
       |
       v
Quality gate
       |
       v
Liveness: lower configurable rate
       |
      PASS
       |
       v
Best-frame selection
       |
       v
ArcFace: once/few times
       |
       v
Embedding
```

The actual detector/liveness frequencies must be determined by device benchmarks rather than hard-coded assumptions.

## 11. Best Frame vs Multi Frame

These modes must be real and intentionally different.

### Hybrid Best Frame

```text
sample N frames
 -> score quality
 -> select best frame
 -> perform one server/client recognition operation
```

### Hybrid Multi Frame

```text
sample N frames
 -> quality filtering
 -> select best K frames
 -> obtain K embeddings or other evidence
 -> aggregate/compare
```

Possible embedding aggregation strategy for experimentation:

```text
E1, E2, E3
 -> L2 normalize individually
 -> average
 -> L2 normalize final vector
```

Any aggregation method must be benchmarked for accuracy and not assumed to improve recognition merely because it uses more frames.

## 12. Mobile GPU/NPU strategy

Unlike the server RTX environment, Android does not have one universal GPU backend. Devices may use Adreno, Mali, Xclipse or other accelerators, and an Android acceleration API may route work to GPU, NPU/DSP or another device accelerator.

Therefore the client must not equate "hardware acceleration" with "GPU" without measuring/identifying the actual backend.

Candidate execution paths include:

```text
CPU
GPU
NNAPI / device accelerator
vendor-specific acceleration where justified
```

The first implementation should establish a reliable CPU baseline, then evaluate available accelerator paths on real devices.

## 13. ONNX/runtime strategy

The current models are ONNX-oriented in the broader project. For Android, a native inference runtime should be selected based on real device support and benchmark results.

Candidate strategy:

```text
Flutter
  |
  v
Android native inference layer
  |
  +--> ONNX Runtime Mobile / Android-compatible ONNX path
  +--> other runtime where justified
  +--> accelerator backend
```

Do not assume that ONNX Runtime automatically means GPU execution. The selected execution provider must be explicitly configured and verified.

Do not introduce multiple ML runtimes simultaneously without a benchmark reason. Each additional runtime increases build size, maintenance and device compatibility complexity.

## 14. Why native Android inference is preferred

For this laboratory client, heavy inference should not be implemented as a Dart-only pipeline when native Android APIs provide better access to:

- CameraX ImageAnalysis
- native buffers
- zero/minimal-copy paths
- accelerator APIs
- native threading
- lifecycle control
- runtime diagnostics

Flutter remains responsible for UI and experiment configuration, with a small platform bridge for inference and telemetry.

## 15. Frame and memory efficiency

Avoid repeated allocations such as:

```text
new Bitmap
new ByteBuffer
new Tensor
GC
```

for every frame.

Prefer reusable buffers and objects where the runtime permits it.

Avoid full-resolution conversion when the model needs only a small input. Convert/crop/resize as close to the native camera/inference boundary as possible.

The detector should receive the minimum useful input size supported by the model while preserving detection quality.

## 16. GPU does not automatically mean better mobile performance

GPU acceleration can be counterproductive for small models if host/device transfer overhead dominates inference.

For every model compare:

```text
CPU inference
vs
GPU inference
vs
NNAPI/device accelerator where available
```

and measure end-to-end time, not only raw model execution.

The objective is:

> Minimum energy and latency per successful verification with acceptable accuracy, not maximum GPU utilization.

## 17. Detection quality issue from previous mobile work

The earlier mobile work observed very small face boxes and unstable/slow apparent detection behavior. Do not assume this is a GPU problem.

Before changing hardware acceleration, instrument and validate:

- camera resolution
- image rotation
- crop coordinates
- aspect ratio
- model input dimensions
- YUV/RGB conversion
- channel order
- normalization
- coordinate mapping from model space to preview space
- detection threshold
- frame timestamps
- inference frequency
- dropped frames
- number of faces

Correctness of the camera/preprocessing pipeline must be established before performance conclusions are drawn.

## 18. Benchmark laboratory

The mobile app should have a developer/laboratory benchmark screen.

Example controls:

```text
Detector
  - YuNet
  - MediaPipe BlazeFace

Liveness
  - MiniFASNetV2

Recognition
  - ArcFace

Backend
  - CPU
  - GPU
  - NNAPI/NPU (if supported)

Mode
  - Full Clip
  - Server Live Stream
  - Client Embedding
  - Hybrid Best Frame
  - Hybrid Multi Frame

Frames / duration
Concurrency
Warm-up
[ Run Benchmark ]
```

The UI should not expose an accelerator option that the current device/runtime cannot support.

## 19. Benchmark telemetry

Every benchmark run should identify:

### Device
- manufacturer
- model
- Android version
- SoC where available
- CPU information
- GPU information
- RAM

### Software
- application version
- Git commit SHA
- Flutter version
- Android Gradle/plugin/runtime versions where useful
- ML runtime version
- model ID/version/hash

### Configuration
- detector
- liveness model
- recognition model
- backend
- precision
- input dimensions
- sampling rates
- concurrency
- mode

### Measurements
- total duration
- frames received
- frames analyzed
- frames dropped/skipped
- detector FPS
- liveness FPS
- recognition count
- preprocessing time
- inference time
- postprocessing time
- end-to-end latency
- P50/P90/P95/P99
- successful verifications
- failed verifications
- CPU utilization where available
- GPU utilization where available
- memory
- thermal/battery observations where available

Results must be exportable as JSON/CSV and readable in the UI.

## 20. Benchmark methodology

Every comparison must use the same:

- input data
- device state
- camera resolution
- lighting conditions where applicable
- model version
- thresholds
- warm-up policy
- number of samples
- measurement window

Separate:

1. Cold-start benchmark.
2. Warm inference benchmark.
3. Sustained benchmark.
4. End-to-end verification benchmark.

Do not compare a cold TensorRT/runtime startup against a warm CPU loop and call the result a model-speed comparison.

## 21. Thermal behavior

Mobile performance can degrade during sustained inference because of thermal throttling.

Benchmark sessions should support sufficiently long runs to detect performance degradation. Where possible record temperature/thermal state and battery impact.

A configuration that is fastest for 10 seconds but throttles heavily after two minutes is not necessarily the best laboratory result.

## 22. Client/server benchmark matrix

The laboratory should eventually compare:

```text
                  Detection    Liveness    Recognition    Network
Full Clip          Server       Server       Server        High
Server Live        Server       Server       Server        Medium/High
Client Embedding   Client       Client/     Client        Low
                             configurable
Hybrid Best        Client       Server       Server        Low/Medium
Hybrid Multi       Client       Server       Server        Medium
```

The exact implementation and API payloads must follow the current server contract.

## 23. Security boundary

Client embedding is an experimental mode.

The server must never assume that:

```text
client says embedding is ArcFace
```

means the embedding was actually generated by ArcFace.

Server-side policy should treat client-provided biometric evidence as untrusted unless an explicit trust mechanism is introduced later.

## 24. Extensibility

Introduce provider interfaces along these lines:

```text
DetectionProvider
LivenessProvider
EmbeddingProvider
```

and result types such as:

```text
DetectionResult
LivenessResult
EmbeddingResult
BenchmarkResult
```

A provider should expose model/backend metadata and timing so the benchmark harness can treat providers uniformly.

## 25. Implementation order

### Phase M1 — Stabilize M05 detector
- Verify CameraX frame handling.
- Verify MediaPipe detector correctness.
- Fix/measure bounding-box mapping.
- Add detailed timing and frame-drop logs.
- Establish CPU/native baseline.

### Phase M2 — Mobile inference abstraction
- Introduce provider interfaces.
- Separate detection, liveness and embedding.
- Separate Flutter UI from native inference implementation.

### Phase M3 — Client Embedding
- Add ArcFace native inference.
- Implement preprocessing/alignment.
- Implement embedding normalization.
- Define and validate server embedding contract.
- Add actual Client Embedding mode.

### Phase M4 — Liveness
- Add MiniFASNetV2 native inference.
- Implement temporal sampling/decision flow.
- Add liveness telemetry.

### Phase M5 — Hardware acceleration
- CPU baseline.
- GPU path where supported.
- NNAPI/NPU path where supported.
- Compare per model and end-to-end.

### Phase M6 — Benchmark harness
- Developer benchmark screen.
- Repeatable workloads.
- JSON/CSV export.
- Percentiles and resource metrics.
- Device/runtime/model metadata.

### Phase M7 — Hybrid modes
- Hybrid Best Frame.
- Hybrid Multi Frame.
- Compare network, latency, accuracy and battery/thermal behavior.

## 26. Non-goals

- The demo is not intended to become a production authentication application in this phase.
- Do not prematurely optimize for one Samsung device.
- Do not assume GPU is always faster.
- Do not force every model onto GPU.
- Do not move all camera frames through Dart unnecessarily.
- Do not add a second inference runtime without a measurable experimental purpose.
- Do not change thresholds merely to obtain better benchmark numbers.

## 27. Working rule for the next implementation chat

Before changing code, inspect the exact current branch and source. Confirm current dependencies, Android native code, Flutter code, model assets and API contracts.

Implement incrementally, run Android builds/tests, run the application on a real device, capture telemetry, compare benchmark results, and only then choose the next optimization.

The final objective is not merely "face detection on a phone". It is:

> **A configurable mobile biometric laboratory that can systematically compare detection, liveness and recognition flows; client/server/hybrid modes; CPU/GPU/NPU execution; model/runtime choices; and performance/accuracy/energy trade-offs, so the best end-to-end flow can be selected from measured evidence.**
