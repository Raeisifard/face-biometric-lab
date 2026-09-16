Continue the existing `face-biometric-lab`.

Phases 00–05 are complete and independently tested.

Implement ONLY the SERVER-CONTROLLED BIOMETRIC POLICY ENGINE.

Do not redesign the existing biometric methods.

Do not remove any existing method.

The goal is to allow the server to determine which capture method the client must use.

SUPPORTED METHODS

At minimum:

SERVER_FULL_CLIP
SERVER_LIVE_STREAM
CLIENT_EMBEDDING
HYBRID_SINGLE_FRAME
HYBRID_MULTI_FRAME

POLICY RESPONSIBILITY

The server determines:

- allowed method
- capture duration
- required frame count
- upload frame rate
- maximum payload size
- required liveness mode
- required quality
- recognition model
- threshold/profile
- fallback method
- session expiration

The client must not unilaterally choose a stronger/weaker method than the server policy.

FLOW

Client
↓
Request biometric verification
↓
Server creates session
↓
Server evaluates policy
↓
Server returns capture policy
↓
Client executes required method
↓
Client submits biometric evidence
↓
Server verifies
↓
Result

POLICY EXAMPLE

Represent policy as versioned configuration.

Example conceptual structure:

policy:
id
version
method
capture
liveness
quality
recognition
security
fallback

Support profiles such as:

NORMAL
HIGH_RISK
VERY_HIGH_RISK

Do not make claims about which profile is appropriate for a real bank without an actual risk policy.

IMPORTANT

Keep policy separate from biometric implementation.

For example:

BiometricPolicyService
should decide:

HYBRID_MULTI_FRAME

while

HybridMultiFrameVerificationService
should execute that method.

Do not put business-risk logic inside OpenCV/model classes.

SESSION BINDING

The selected policy must be bound to the verification session.

The server must reject submissions that do not conform to the active policy.

For example:

If policy requires 4 frames,
a client submitting 1 frame must not silently downgrade to single-frame verification.

FALLBACK

Support a policy-defined fallback.

Example:

HYBRID_SINGLE_FRAME
↓
if INCONCLUSIVE
↓
HYBRID_MULTI_FRAME

But do not implement full adaptive orchestration yet.

This phase only defines policy and validates policy compliance.

TESTING

Test:

- policy selection
- policy versioning
- session binding
- unsupported method
- invalid parameters
- frame count violation
- size violation
- duration violation
- liveness requirement violation
- model mismatch
- fallback declaration

SIMULATOR

Display the active server policy before capture.

Show:

Method
Duration
Frame count
Liveness mode
Model profile
Maximum payload
Session expiration

The UI must make clear that these values are server-issued.

Do not allow the simulator to silently override them.