# Phase 06 — Server-Controlled Biometric Policy Engine

Phase 06 adds a server-side policy layer without moving biometric/business-risk logic into OpenCV or ONNX classes.

## Policy flow

Client
  -> POST /api/v1/biometric/policy/sessions
  -> server-issued policy snapshot
  -> method-specific capture/session
  -> policy validation
  -> existing biometric implementation
  -> verification result

The policy snapshot contains:
- policy id and version
- profile
- selected method
- capture duration and frame constraints
- upload FPS and maximum payload
- liveness mode/requirement
- minimum quality
- recognition model and threshold
- declared fallback
- session TTL

Supported methods:
- FULL_CLIP / SERVER_FULL_CLIP
- LIVE_STREAM / SERVER_LIVE_STREAM
- CLIENT_EMBEDDING
- HYBRID_SINGLE_FRAME
- HYBRID_MULTI_FRAME

FREE_METHOD is treated as a legacy configuration value. The policy engine resolves it to a concrete server-selected method instead of exposing client method choice.

## Endpoints

GET /api/v1/biometric/policy
POST /api/v1/biometric/policy/sessions?referenceId=test-person-01
GET /api/v1/biometric/policy/sessions/{sessionId}
POST /api/v1/biometric/policy/validate

The diagnostics endpoint validates policy inputs without invoking biometric models. The simulator uses it for method, frame-count, payload, duration, quality, liveness, and model-mismatch tests.

## Profiles

The development configuration includes NORMAL, HIGH_RISK, and VERY_HIGH_RISK. These are implementation/test profiles only; they are not recommendations for a production banking risk policy.

Environment overrides:
- BIOMETRIC_POLICY_PROFILE
- BIOMETRIC_POLICY_METHOD
- BIOMETRIC_HIGH_RISK_METHOD
- BIOMETRIC_VERY_HIGH_RISK_METHOD

The existing biometric implementation remains responsible for detection, quality measurement, liveness inference, embedding generation, and matching. The policy layer only decides and validates the contract.

Fallback is represented as policy metadata in this phase. Adaptive retry/escalation orchestration is deliberately deferred to the next phase.