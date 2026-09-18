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

## Development versus production method selection

The environment is intentionally separated from the biometric risk profile.

- `dev` Spring profile sets `biometric.policy.selection-mode=CLIENT_SELECTABLE`.
- In dev, the simulator may request `FULL_CLIP`, `LIVE_STREAM`, `CLIENT_EMBEDDING`, `HYBRID_SINGLE_FRAME`, or `HYBRID_MULTI_FRAME`.
- The policy API resolves the requested method to its corresponding method policy and returns that policy to the simulator.
- Verification controllers also resolve the policy from the selected method, so changing the simulator method changes the policy contract used for that request.
- Production uses `SERVER_ASSIGNED`. A client-supplied method is treated as a request/hint only; it is accepted only when it matches the server-assigned policy. It cannot downgrade or replace the server policy.

Start the service with `SPRING_PROFILES_ACTIVE=dev` for free simulator testing. Do not use the dev profile as a production risk-policy concept.

### Method policy API

`GET /api/v1/biometric/policy?method=HYBRID_MULTI_FRAME`

returns the policy associated with that method in `CLIENT_SELECTABLE` mode.

`POST /api/v1/biometric/policy/sessions?referenceId=test-person-01&requestedMethod=HYBRID_MULTI_FRAME`

creates a policy session bound to the requested method's policy.

This preserves the important production property: the client never defines the security constraints; it only requests a mode in the explicitly controlled development environment.
