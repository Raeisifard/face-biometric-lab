# Canonical Biometric Verification Response

All biometric **verification** modes expose the same response contract. Method 5 adds `HYBRID_MULTI_FRAME` while preserving the same envelope and result/status semantics:

- `POST /api/v1/biometric/verify` — embedding verification
- `POST /api/v1/biometric/verify-embedding` — embedding compatibility endpoint
- `POST /api/v1/biometric/video-verification/verify-clip` — full-clip verification
- `POST /api/v1/biometric/hybrid-verification/verify` — hybrid best-frame verification
- `POST /api/v1/biometric/hybrid-multi-frame-verification/verify` — hybrid multi-frame server re-validation
- `POST /api/v1/live-stream/sessions/{sessionId}/complete` — live-stream final result

Session creation, frame-upload feedback, model discovery, and enrollment remain workflow-specific APIs; they are not final verification outcomes.

## Embedding model contract

Embedding verification is model-aware. A request carrying a vector must explicitly identify:

```text
modelId
modelVersion
dimension
```

The server resolves the tuple through the biometric model registry. Equal vector dimensions are not sufficient to make two embeddings compatible. For example, the 512-D MobileFaceNet and 512-D ArcFace/R50 profiles are separate embedding spaces and require separate reference profiles.

The server validates the selected model, vector dimension, finite values, normalization declaration, and L2 norm before comparing the probe with a reference. It never pads, truncates, converts, or otherwise maps one model's embedding into another model's space.

## Method 5 flow

```text
Browser / mobile-style simulator
  camera -> detection -> tracking/quality -> client liveness -> select N frames
       |
       +-- ordered images + sequence number + timestamp
       v
Server session
  validate session/expiry/replay/frame count/sequence
       |
       v
For every submitted frame
  decode -> face count -> quality -> server liveness -> alignment -> ArcFace embedding -> 1:1 similarity
       |
       v
Aggregate only valid/high-quality frame evidence
  mean | median | min | max
       |
       v
Final canonical VerificationResponse
```

The client liveness result is informative only. Method 5 independently repeats the security-sensitive checks on the server. The server also requires ordered sequence metadata and consumes a short-lived verification session to reduce arbitrary/replayed submissions.

The current development implementation uses passive server liveness and aggregates the independently validated frame evidence. It does **not** claim that multiple unrelated still images are equivalent to a genuine temporal live sequence; stronger temporal/challenge PAD can be added behind the same contract later.

## Response shape

```json
{
  "requestId": "uuid",
  "referenceId": "user-123",
  "captureMethod": "HYBRID_MULTI_FRAME",
  "result": "MATCH",
  "code": "MATCH",
  "message": "Biometric verification matched the enrolled reference.",
  "httpStatus": 200,
  "similarity": 0.7587,
  "threshold": 0.65,
  "model": {
    "modelId": "arcface-512",
    "modelVersion": "w600k-r50",
    "dimension": 512,
    "algorithm": "COSINE"
  },
  "quality": {
    "qualityScore": 0.65,
    "live": true,
    "livenessScore": 0.99
  },
  "metrics": {
    "processingTimeMs": 611,
    "decodedFrames": 4,
    "recognitionFrames": 4,
    "uploadedBytes": 278108
  },
  "reasonCodes": []
}
```

Clients should use `result` for the high-level verification outcome and `code` for the machine-readable reason. Optional metrics are `null` when a capture mode does not produce that metric.

## Result semantics

| result | Meaning | Typical HTTP status |
|---|---|---:|
| `MATCH` | Reference exists and aggregated similarity meets the threshold. | 200 |
| `NO_MATCH` | Reference exists, comparison was performed, and aggregated similarity is below the threshold. | 200 |
| `INCONCLUSIVE` | Verification could not produce a reliable match decision. | 200 |
| `INVALID_REQUEST` | Request/input is invalid and verification should not be attempted. | 400 |
| `PROCESSING_ERROR` | Server-side processing/model failure. | 500 |

For the model compatibility case, `MODEL_MISMATCH` is represented as `INVALID_REQUEST` with **HTTP 409 Conflict**. The response body remains the canonical verification envelope so clients can display the actual reason instead of an empty/null result.

`NO_MATCH` must never be synthesized from a missing reference. If no compatible reference embedding exists, the service returns `INCONCLUSIVE` with `REFERENCE_NOT_FOUND` and `similarity: null`, because no cosine comparison occurred.

## Standard machine-readable codes

### Successful / decision codes

- `MATCH`
- `SIMILARITY_BELOW_THRESHOLD`

### Reference/model codes

- `REFERENCE_NOT_FOUND`
- `MODEL_MISMATCH`
- `MODEL_METADATA_REQUIRED`

### Capture / biometric evidence codes

- `NO_FACE`
- `MULTIPLE_FACES`
- `FACE_TOO_SMALL`
- `LOW_QUALITY`
- `LIVENESS_FAILED`
- `NO_VALID_FRAMES`
- `FRAME_COUNT_MISMATCH`
- `INVALID_SEQUENCE`

### Request / transport-input codes

- `CAPTURE_REQUIRED`
- `CAPTURE_TYPE_NOT_SUPPORTED`
- `CAPTURE_MODE_NOT_ALLOWED`
- `INVALID_IMAGE`
- `INVALID_IMAGE_TYPE`
- `INVALID_VIDEO`
- `IMAGE_TOO_LARGE`
- `VIDEO_TOO_LARGE`
- `VIDEO_TOO_LONG`
- `SUSPICIOUS_IMAGE_DIMENSIONS`
- `EMBEDDING_DIMENSION_MISMATCH`
- `NORMALIZED_EMBEDDING_REQUIRED`
- `INVALID_EMBEDDING`
- `SESSION_NOT_FOUND`
- `SESSION_EXPIRED`
- `SESSION_REPLAYED`
- `REFERENCE_ID_MISMATCH`

### Processing codes

- `MODEL_ERROR`
- `VIDEO_VERIFICATION_DISABLED`
- `HYBRID_MULTI_FRAME_DISABLED`

## Method 5 session contract

The simulator requests a frame count from 1 upward. The server enforces its configured maximum and binds the accepted count to the short-lived session. Development defaults are **4 frames**, maximum **8 frames**, passive server liveness, and `MEAN` similarity aggregation. The session is single-use.

Every uploaded frame carries an ordered sequence number and timestamp. The server rejects missing/out-of-order metadata instead of silently treating unrelated images as one capture sequence.

## Client rule

A client should not implement separate response parsers for the biometric methods. Parse the canonical envelope first, then branch only on:

```text
result
code
```

Use nested `quality`, `model`, and `metrics` only for display, diagnostics, policy decisions, or telemetry. Never infer `MATCH` from a non-null similarity alone; use `result`.
