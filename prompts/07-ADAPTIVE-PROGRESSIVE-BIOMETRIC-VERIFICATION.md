Continue the existing `face-biometric-lab`.

Phases 00–06 are complete and tested.

Implement the final orchestration layer:

ADAPTIVE / PROGRESSIVE 1:1 BIOMETRIC VERIFICATION

Do not replace the existing biometric methods.

Do not duplicate their implementation.

The orchestration layer should compose the already implemented methods.

OBJECTIVE

The server should be able to start with a lower-cost verification method and escalate to a stronger method when the result is insufficient or when policy requires it.

CONCEPT

Initial method
↓
Verification
↓
┌─────────────────────────────┐
│ MATCH                       │
│ NO_MATCH                    │
│ INCONCLUSIVE                │
│ ERROR                       │
└─────────────────────────────┘
│
▼
If escalation allowed
│
▼
Stronger method
│
▼
Final result

IMPORTANT

Do not automatically interpret:

NO_MATCH

as:

INCONCLUSIVE.

They are different outcomes.

Only policy-defined conditions should cause escalation.

Example:

HYBRID_SINGLE_FRAME
↓
INCONCLUSIVE
↓
HYBRID_MULTI_FRAME
↓
INCONCLUSIVE
↓
SERVER_FULL_CLIP

Another policy could start directly with:

SERVER_LIVE_STREAM

or:

SERVER_FULL_CLIP

ESCALATION REASONS

Support machine-readable reasons such as:

LOW_QUALITY
INSUFFICIENT_LIVENESS_EVIDENCE
LOW_RECOGNITION_CONFIDENCE
CAPTURE_INTERRUPTED
INSUFFICIENT_FRAMES
POLICY_REQUIRED

Do not expose raw model internals unnecessarily.

STATE MACHINE

Create an explicit verification state machine.

Example conceptual states:

CREATED
POLICY_ASSIGNED
CAPTURING
PROCESSING
EVALUATED
ESCALATING
COMPLETED
EXPIRED
FAILED

Every transition must be controlled and auditable.

IMPORTANT SECURITY RULE

A client must never be able to request:

"Please downgrade to a cheaper method."

The server policy controls the allowed transitions.

AUDIT

Record non-sensitive audit information:

- verification session ID
- policy ID/version
- method used
- escalation reason
- timestamps
- final status
- model/version
- processing metrics

Do not log raw face images or embeddings by default.

PRIVACY

Raw biometric material should not be retained unless explicitly required by policy.

Make retention configurable.

Do not persist captured video/images merely for debugging.

TESTING

Build end-to-end tests for at least:

1. single-frame succeeds
2. single-frame inconclusive → multi-frame
3. multi-frame succeeds
4. multi-frame inconclusive → full clip
5. no fallback allowed
6. session expiration
7. policy violation
8. client attempts downgrade
9. client attempts replay
10. final NO_MATCH
11. final MATCH
12. final INCONCLUSIVE

SIMULATOR

The UI should show the verification progression:

Step 1
Capture

Step 2
Verification

Step 3
Escalation if required

Step 4
Final result

For development, display:

method
policy
reason for escalation
latency
frame count
final score

Do not expose sensitive biometric internals in a production-style UI.

DELIVERABLE

At the end, provide:

- architecture diagram
- state machine
- policy interaction
- API flow
- test matrix
- performance comparison between all methods
- security considerations
- limitations
- recommended next engineering steps

Do not implement 1:N identification.
This entire project remains strictly 1:1 face verification.