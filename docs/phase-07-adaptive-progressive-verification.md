# Phase 07 — Adaptive / Progressive 1:1 Biometric Verification

Phase 07 adds server-controlled orchestration over the existing verification methods. The existing detection, quality, liveness, embedding and matching implementations are reused; this layer decides when a completed attempt may escalate.

## Architecture

\`\`\`text
Client / Simulator
       |
       v
AdaptiveVerificationController
       |
       v
AdaptiveVerificationService
       |
       +--> BiometricPolicyService ----> versioned method policy
       |
       +--> HYBRID_SINGLE_FRAME ------> VideoVerificationEngine
       +--> HYBRID_MULTI_FRAME --------> HybridMultiFrameVerificationService
       +--> SERVER_FULL_CLIP ----------> VideoClipDecoder + VideoVerificationEngine
       +--> CLIENT_EMBEDDING ----------> FaceMatchingService
       +--> SERVER_LIVE_STREAM --------> VideoVerificationEngine live-frame path
       |
       v
canonical adaptive state + audit-safe history
\`\`\`

## Progressive chain

The development configuration now expresses a monotonic escalation chain:

\`\`\`text
CLIENT_EMBEDDING -> HYBRID_SINGLE_FRAME -> HYBRID_MULTI_FRAME -> SERVER_FULL_CLIP
SERVER_LIVE_STREAM -> HYBRID_MULTI_FRAME -> SERVER_FULL_CLIP
\`\`\`

\`SERVER_FULL_CLIP\` has no fallback. An escalation target is resolved from server configuration with \`escalationPolicyForMethod\`; a client cannot select an escalation target directly.

## State machine

\`\`\`text
CREATED
  |
  v
POLICY_ASSIGNED -> CAPTURING -> PROCESSING -> EVALUATED
                                           |
                         +-----------------+----------------+
                         |                                  |
                   MATCH / NO_MATCH                    INCONCLUSIVE
                         |                                  |
                         v                                  v
                    COMPLETED                       escalation allowed?
                                                   /              \\
                                                 yes              no
                                                  |                |
                                                  v                v
                                             ESCALATING       COMPLETED
                                                  |
                                                  v
                                             POLICY_ASSIGNED
                                                  |
                                                  v
                                              CAPTURING

Any non-recoverable processing failure -> FAILED
Expired session -> EXPIRED
\`\`\`

\`NO_MATCH\` is terminal for the current verification policy unless the underlying method itself explicitly reports an \`INCONCLUSIVE\` result. The orchestrator never rewrites \`NO_MATCH\` into \`INCONCLUSIVE\`.

## API flow

1. \`POST /api/v1/biometric/adaptive/sessions?referenceId=...&requestedMethod=...\`
2. \`GET /api/v1/biometric/adaptive/sessions/{sessionId}\` for state/progression.
3. Submit the current method's evidence:
   - \`/sessions/{id}/image\` for \`HYBRID_SINGLE_FRAME\` or \`SERVER_LIVE_STREAM\`
   - \`/sessions/{id}/frames\` for \`HYBRID_MULTI_FRAME\`
   - \`/sessions/{id}/clip\` for \`SERVER_FULL_CLIP\`
   - \`/sessions/{id}/embedding\` for \`CLIENT_EMBEDDING\`
4. If the result is policy-escalatable \`INCONCLUSIVE\`, the response sets \`escalated=true\` and exposes the next method. The client then submits evidence for that method.
5. Poll the session or use the attempt response to display the final state.

## Escalation conditions

Only machine-readable, policy-compatible inconclusive reasons can escalate. The implementation currently recognizes:

- \`LOW_QUALITY\`
- \`INSUFFICIENT_LIVENESS_EVIDENCE\`
- \`LIVENESS_FAILED\`
- \`LOW_RECOGNITION_CONFIDENCE\`
- \`CAPTURE_INTERRUPTED\`
- \`INSUFFICIENT_FRAMES\`
- \`NO_FACE\`
- \`MULTIPLE_FACES\`
- \`FACE_TOO_SMALL\`
- \`NO_VALID_FRAMES\`

Reference absence, model mismatch and arbitrary client errors are not treated as reasons to silently downgrade or loop through methods.

## Test matrix

| Scenario | Expected behavior |
|---|---|
| Single frame succeeds | \`MATCH\` or \`NO_MATCH\`, \`COMPLETED\` |
| Single frame inconclusive / low quality | Escalate to multi-frame |
| Multi-frame succeeds | \`COMPLETED\` |
| Multi-frame inconclusive | Escalate to full clip |
| No fallback configured | Remain \`COMPLETED\` with \`INCONCLUSIVE\` |
| Session expired | \`EXPIRED\` |
| Policy violation | Request rejected; no method substitution |
| Client requests downgrade | Rejected by server-assigned policy |
| Replay after terminal result | Rejected as terminal session |
| Final no-match | \`NO_MATCH\`, \`COMPLETED\`; no automatic escalation |
| Final match | \`MATCH\`, \`COMPLETED\` |
| Final inconclusive | \`INCONCLUSIVE\`, \`COMPLETED\` |

## Audit and privacy

Each session records only audit-safe metadata: session ID, policy ID/version, method, transition, reason, timestamps, final result and aggregate processing counters. Raw face images, video and embeddings are not stored by this orchestration layer.

## Performance

The orchestration layer adds only policy/state-management overhead around the selected biometric method. Actual biometric latency remains dominated by the selected detector/liveness/recognition path. Progressive verification can therefore trade additional latency and compute for another opportunity to obtain sufficient evidence; the exact latency must be measured with the real models, camera/device and deployment hardware rather than estimated statically.

## Limitations

- Adaptive execution currently composes the server-side service interfaces already present in the repository; it does not invent a new biometric model.
- Browser capture still has to provide the correct evidence for the currently assigned method.
- Session state is in-memory and therefore not suitable for multi-node production deployment without an external state store.
- Escalation policy should eventually become explicitly versioned data rather than a fixed reason allow-list.
- Production replay protection, nonce/challenge binding and client/device attestation remain separate security layers.
- This phase remains strictly 1:1 face verification; no 1:N identification is introduced.
