# Canonical Biometric Verification Response

All biometric **verification** modes now expose the same response contract:

- `POST /api/v1/biometric/verify` — embedding verification
- `POST /api/v1/biometric/verify-embedding` — embedding compatibility endpoint
- `POST /api/v1/biometric/video-verification/verify-clip` — full-clip verification
- `POST /api/v1/biometric/hybrid-verification/verify` — hybrid best-frame verification
- `POST /api/v1/live-stream/sessions/{sessionId}/complete` — live-stream final result

Session creation, frame-upload feedback, model discovery, and enrollment remain workflow-specific APIs; they are not final verification outcomes.

## Response shape

```json
{
  "requestId": "uuid",
  "referenceId": "user-123",
  "captureMethod": "HYBRID_SINGLE_FRAME",
  "result": "MATCH",
  "code": "MATCH",
  "message": "Biometric verification matched the enrolled reference.",
  "httpStatus": 200,
  "similarity": 0.7587,
  "threshold": 0.50,
  "model": {
    "modelId": "arcface-512",
    "modelVersion": "w600k-r50",
    "dimension": 512,
    "algorithm": "COSINE"
  },
  "quality": {
    "qualityScore": 0.65,
    "live": null,
    "livenessScore": 0.99
  },
  "metrics": {
    "processingTimeMs": 611,
    "decodedFrames": null,
    "recognitionFrames": null,
    "uploadedBytes": 69527
  },
  "reasonCodes": []
}
```

Clients should use `result` for the high-level verification outcome and `code` for the machine-readable reason. Optional metrics are `null` when a capture mode does not produce that metric.

## Result semantics

| result | Meaning | Typical HTTP status |
|---|---|---:|
| `MATCH` | Reference exists and similarity meets the threshold. | 200 |
| `NO_MATCH` | Reference exists, comparison was performed, and similarity is below the threshold. | 200 |
| `INCONCLUSIVE` | Verification could not produce a reliable match decision. | 200 |
| `INVALID_REQUEST` | Request/input is invalid and verification should not be attempted. | 400 |
| `PROCESSING_ERROR` | Server-side processing/model failure. | 500 |

`NO_MATCH` must never be synthesized from a missing reference. If no compatible reference embedding exists, the service returns `INCONCLUSIVE` with `REFERENCE_NOT_FOUND` and `similarity: null`, because no cosine comparison occurred.

## Standard machine-readable codes

### Successful / decision codes

- `MATCH`
- `SIMILARITY_BELOW_THRESHOLD`

### Reference/model codes

- `REFERENCE_NOT_FOUND` — no enrolled reference for the requested model ID/version.
- `MODEL_MISMATCH` — probe embedding and server verification model are incompatible.

### Capture / biometric evidence codes

- `NO_FACE`
- `MULTIPLE_FACES`
- `FACE_TOO_SMALL`
- `LOW_QUALITY`
- `LIVENESS_FAILED`
- `TEMPORAL_EVIDENCE_PENDING`
- `RECOGNITION_PENDING`

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

## Client rule

A client should not implement separate response parsers for the biometric methods. Parse the canonical envelope first, then branch only on:

```text
result
code
```

Use nested `quality`, `model`, and `metrics` only for display, diagnostics, policy decisions, or telemetry. Never infer `MATCH` from a non-null similarity alone; use `result`.
