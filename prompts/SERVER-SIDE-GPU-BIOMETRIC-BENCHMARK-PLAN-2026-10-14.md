# Server-Side Face Biometric GPU, Throughput & Benchmark Plan

**Date:** 2026-10-14  
**Project:** `face-biometric-lab`  
**Scope:** `face_biometric_service` / server-side inference  
**Status:** Planning baseline for the next implementation chat

## 1. Purpose

This document is the working session record and implementation prompt for the server side of `face-biometric-lab`. The service must evolve from a functional biometric service into a configurable laboratory that can determine the best inference flow, runtime, GPU backend, concurrency, precision, batching and deployment settings through repeatable benchmarks.

The primary target is NVIDIA RTX GPUs, especially RTX 40xx and RTX 50xx families, while retaining a clean CPU fallback and avoiding hard-coding the application to one GPU model.

## 2. Core principles

- GPU acceleration is an implementation concern, not business logic.
- The service must remain functional without a GPU.
- CUDA support is the first GPU milestone.
- TensorRT is an optimization/backend option, not a mandatory first dependency.
- The application must be able to compare CPU, CUDA and TensorRT execution.
- Do not assume that more Java threads means more GPU throughput.
- Optimize the complete verification pipeline, not GPU utilization in isolation.
- Benchmark before and after every performance change.
- Preserve model correctness and biometric accuracy while optimizing performance.
- Model metadata, preprocessing, normalization, thresholds and embedding semantics must remain explicit and versioned.
- GPU-specific code must be isolated behind inference abstractions.

## 3. Current/target model set

The biometric pipeline is expected to contain:

- Face detection: YuNet and/or other detector implementations.
- Face recognition/embedding: ArcFace, currently based on the project recognition model configuration (512-dimensional normalized embedding is the intended contract where applicable).
- Face liveness/anti-spoofing: MiniFASNetV2.

Each model must be independently benchmarkable. A model being small or fast on CPU does not automatically mean that moving it to GPU improves end-to-end performance.

## 4. Target server architecture

```text
HTTP/API
   |
   v
Verification / application layer
   |
   v
Inference orchestration
   |
   +--> DetectionProvider
   +--> LivenessProvider
   +--> EmbeddingProvider
              |
              v
        OnnxSessionFactory
              |
      +-------+----------------+
      |       |                |
     CPU    CUDA           TensorRT
      |       |                |
      +-------+----------------+
              |
          NVIDIA GPU
```

Business logic must not directly create or configure ONNX Runtime sessions. Introduce an inference/session factory and provider abstraction so the same verification flow can run with different execution backends.

## 5. CUDA vs TensorRT decision

CUDA is the first implementation target and is sufficient for the first GPU-enabled version.

CUDAExecutionProvider means ONNX Runtime can execute supported model operations using the NVIDIA GPU through CUDA/cuDNN.

TensorRTExecutionProvider is a second optimization path. TensorRT can optimize/compile supported portions of an ONNX graph for the target NVIDIA GPU and can exploit optimized kernels and reduced precision such as FP16 and, where validated, INT8.

Recommended progression:

1. CPU baseline.
2. ONNX Runtime + CUDAExecutionProvider.
3. Tune CUDA execution and concurrency.
4. Benchmark TensorRT against CUDA.
5. Evaluate FP16.
6. Evaluate INT8 only after accuracy validation.
7. Select the best configuration based on measured end-to-end results.

CUDA and TensorRT should be treated as complementary execution providers. TensorRT should be allowed to fall back to CUDA for unsupported operations where the runtime/configuration supports this arrangement.

Do not make TensorRT a mandatory dependency until benchmarks demonstrate meaningful benefit relative to its operational complexity.

## 6. RTX 40xx / 50xx compatibility requirement

The service must support a range of NVIDIA RTX 40xx and 50xx GPUs rather than targeting one exact GPU.

The implementation must avoid hard-coded assumptions about:

- GPU model name
- compute capability
- VRAM size
- tensor-core generation
- optimal batch size
- optimal concurrency
- precision support
- TensorRT engine portability

At startup and/or benchmark time, record GPU identity and relevant runtime capabilities. The benchmark system must identify the GPU and associate results with it.

TensorRT engines are hardware/runtime sensitive; do not treat an engine built for one GPU as universally portable. Prefer runtime/build-time engine generation or a clearly versioned engine cache strategy.

## 7. Configuration design

The inference backend should be configurable, for example:

```yaml
biometric:
  inference:
    provider: cuda       # cpu | cuda | tensorrt | auto
    precision: fp32      # fp32 | fp16 | int8 (when supported/validated)
    mode: low-latency   # low-latency | high-throughput
    batching:
      enabled: false
      max-size: 1
```

Exact property names should follow the existing project conventions and must be verified against the actual source before implementation.

An `auto` mode may eventually select:

```text
TensorRT available and validated -> TensorRT
otherwise CUDA available        -> CUDA
otherwise                        -> CPU
```

Automatic selection must never silently change biometric model semantics or thresholds.

## 8. Session lifecycle

Do not create an ONNX session per request.

Sessions should be initialized once and reused. The session factory should create the required model sessions at application startup or controlled lazy initialization, then expose thread-safe inference services.

The implementation must verify ONNX Runtime session thread-safety and provider behavior rather than assuming that one mutable session can safely be used in arbitrary concurrent patterns.

If experiments show that a pool of sessions/streams provides better throughput, make that a controlled configurable strategy and benchmark it against a single shared session.

## 9. Multithreading and concurrency

There are at least three different concurrency layers:

1. HTTP/request concurrency.
2. CPU preprocessing concurrency.
3. GPU inference concurrency.

Do not multiply these blindly.

For example, if 20 requests each cause an ORT configuration with 8 internal threads, the effective CPU thread pressure can become excessive. ONNX Runtime has its own intra-op/inter-op threading controls.

The implementation must explicitly measure:

- HTTP concurrency.
- executor queue depth.
- active inference count.
- ORT intra-op threads.
- ORT inter-op threads.
- CPU utilization.
- GPU utilization.
- GPU memory usage.
- inference latency.
- throughput.

Recommended initial strategy:

```text
HTTP layer
   |
bounded work queue
   |
controlled verification jobs
   |
shared/reused inference sessions
```

Start conservatively with concurrency 1, then test 2, 4, 8, etc. Do not assume the optimal number.

## 10. Batching

Batching must be investigated because maximum throughput is different from minimum latency.

Benchmark at least:

```text
batch = 1
batch = 2
batch = 4
batch = 8
```

where the model and runtime support the selected shape.

For low-latency interactive verification, batch 1 may remain optimal. For laboratory/high-throughput workloads, batching may improve GPU utilization substantially.

The implementation should support a future batching scheduler that groups compatible inference requests for a bounded time window without causing unacceptable latency.

## 11. Pipeline optimization

The end-to-end pipeline should be treated as:

```text
image/video input
 -> decode
 -> resize
 -> crop/alignment
 -> tensor creation
 -> host/device transfer
 -> inference
 -> postprocess
 -> embedding/decision
```

Do not optimize only the model inference time.

The benchmark must separate at least:

- input/decode time
- preprocessing time
- host-to-device transfer where measurable
- model inference time
- device-to-host transfer where measurable
- postprocessing time
- total request latency

ONNX Runtime CUDA I/O Binding should be considered when memory-transfer overhead becomes significant.

## 12. Precision

Benchmark FP32 first.

Then evaluate FP16 for compatible models and GPUs. FP16 may improve throughput and reduce memory pressure, especially on RTX Tensor Cores, but accuracy must be checked.

INT8 should be treated as an advanced optimization. For biometric embeddings and liveness decisions, quantization must be validated against a representative accuracy/ROC/FAR/FRR test set before adoption.

Never change precision in production merely because latency improved.

## 13. Benchmark laboratory

The server must become a laboratory, not just a service.

A benchmark run should record:

### Hardware
- GPU vendor/model.
- VRAM.
- driver/runtime versions where available.
- CPU model/core count.
- RAM.

### Software
- OS.
- Java version.
- Spring Boot version.
- ONNX Runtime version.
- CUDA/cuDNN versions where observable.
- TensorRT version when used.
- model file hash/version.
- application commit SHA.

### Configuration
- execution provider.
- precision.
- batch size.
- concurrency.
- ORT threading.
- image/input dimensions.
- model configuration.

### Measurements
- requests completed.
- errors.
- dropped/queued work.
- average latency.
- P50/P90/P95/P99 latency.
- min/max latency.
- throughput (requests/sec).
- CPU utilization.
- GPU utilization.
- VRAM.
- thermal/throttling information when available.
- accuracy/decision metrics for a known test set.

The benchmark result must be exportable to a machine-readable format such as JSON/CSV and should also have a human-readable summary.

## 14. Benchmark scenarios

At minimum:

1. Single request latency.
2. Sustained low-concurrency throughput.
3. Increasing concurrent requests.
4. Batch-size comparison.
5. CPU vs CUDA.
6. CUDA vs TensorRT.
7. FP32 vs FP16.
8. Optional INT8 after accuracy validation.
9. Warm model vs cold startup.
10. Mixed detection/liveness/recognition workload.

## 15. Accuracy/performance guardrails

Every optimization benchmark must have a correctness gate.

For recognition, compare embeddings and verification decisions against a known reference dataset.

For liveness, compare genuine/spoof classification metrics.

For detection, compare face count, box quality and stability.

Performance improvements that materially change biometric outcomes must be rejected or explicitly documented.

## 16. Low-latency vs high-throughput modes

The service should eventually expose two tuning profiles:

```text
low-latency
- batch 1
- bounded concurrency
- minimal queueing
- fast response

high-throughput
- batching enabled where beneficial
- higher controlled concurrency
- queue-based scheduling
- maximize requests/sec subject to latency SLA
```

The actual settings must be determined empirically per GPU class.

## 17. GPU portability strategy

Do not write code such as:

```text
if GPU == RTX 5060 then batch = 8
```

Instead maintain capability/profile data and benchmark-derived recommendations, for example:

```text
GPU family/model
 -> supported provider
 -> supported precision
 -> tested batch sizes
 -> tested concurrency
 -> measured throughput
 -> measured latency
```

The laboratory should be able to discover and record the best configuration for a new RTX 40xx/50xx machine rather than requiring source-code changes.

## 18. Operational fallback

The service must remain operational when:

- no NVIDIA GPU is present;
- CUDA is unavailable;
- TensorRT is unavailable;
- a model is unsupported by a selected provider;
- GPU memory is insufficient;
- an optimized engine cannot be built/loaded.

Fallback policy must be explicit and observable. Do not silently hide provider failures.

## 19. Implementation order

### Phase S1 — Baseline
- Inspect current inference classes and dependencies.
- Establish CPU benchmark.
- Capture current model metadata and accuracy baseline.

### Phase S2 — CUDA
- Add the appropriate ONNX Runtime GPU dependency/version.
- Verify CUDA/cuDNN compatibility for the supported deployment matrix.
- Add provider abstraction/factory.
- Implement CUDA execution.
- Add startup diagnostics.

### Phase S3 — Concurrency
- Reuse sessions.
- Add bounded executor/controlled concurrency.
- Tune ORT threading.
- Benchmark concurrency levels.

### Phase S4 — Benchmark harness
- Add standardized benchmark request/workload.
- Add latency percentiles, throughput and resource telemetry.
- Persist/export results.

### Phase S5 — TensorRT
- Add TensorRT as an optional provider.
- Build/load optimized engines as appropriate.
- Benchmark CUDA vs TensorRT.
- Validate fallback behavior.

### Phase S6 — Precision/batching
- FP16 benchmark.
- Batch benchmark.
- INT8 only after biometric accuracy validation.

### Phase S7 — Production profiles
- Define hardware/runtime profiles for RTX 40xx/50xx.
- Add low-latency and high-throughput profiles.
- Document recommended settings from measured results.

## 20. Non-goals for the first server GPU implementation

- Do not redesign biometric business rules merely to enable GPU.
- Do not require TensorRT before CUDA has a measured baseline.
- Do not assume all models should execute on GPU.
- Do not maximize thread count without benchmark evidence.
- Do not optimize away correctness checks.
- Do not make the service dependent on one exact NVIDIA GPU.

## 21. Working rule for the next implementation chat

Before modifying code, inspect the current `face_biometric_service` source, Gradle/Maven dependencies, model-loading code and existing tests on the target branch. Do not rely on this plan as a substitute for current source inspection.

Implement one measurable step at a time, run tests/build, run a benchmark where applicable, inspect the result, then proceed.

The final objective is not simply "GPU enabled". The objective is:

> **A portable, observable, benchmark-driven server-side biometric inference laboratory that can determine the best CPU/GPU/TensorRT configuration, concurrency, batching and precision for RTX 40xx/50xx hardware while preserving biometric correctness.**
