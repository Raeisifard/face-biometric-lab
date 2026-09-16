Continue the existing `face-biometric-lab`.

Phases 00–04 are complete and tested.

Implement ONLY:

METHOD 5 — HYBRID MULTI-FRAME SERVER RE-VALIDATION

OBJECTIVE

The client performs lightweight capture processing:

- face detection
- tracking
- quality analysis
- client-side liveness
- frame selection

Instead of sending one frame, the client sends a configurable number of high-quality images.

Default development configuration:

3 or 4 frames

The number must be configurable.

The server independently repeats the security-sensitive processing.

FLOW

CLIENT

Camera
↓
Detection
↓
Tracking
↓
Quality
↓
Liveness
↓
Select N frames
↓
Send frames

SERVER

Receive N images
↓
Validate session
↓
Decode
↓
Detect face on each frame
↓
Validate face count
↓
Quality analysis
↓
Liveness across sequence
↓
Alignment
↓
Recognition
↓
Generate embeddings
↓
Calculate similarities
↓
Aggregate evidence
↓
Final 1:1 verification

IMPORTANT

Sending multiple images does not automatically make the system spoof-resistant.

The server must evaluate temporal/sequence evidence where the selected frames preserve meaningful temporal relationships.

Do not treat four unrelated still images as equivalent to a genuine live sequence.

SERVER LIVENESS

The server must support a configurable liveness mode.

At minimum architect for:

PASSIVE
ACTIVE/CHALLENGE

Do not implement unnecessary complex challenge protocols unless justified by the existing liveness model.

RECOGNITION

For each suitable frame:

image
↓
alignment
↓
embedding
↓
comparison

Then aggregate evidence.

Make the aggregation strategy configurable, for example:

- mean similarity
- median similarity
- minimum similarity
- weighted quality score
- configurable acceptance rule

Do not simply average bad frames with good frames.

Only valid/high-quality frames should contribute according to policy.

SECURITY

The server must independently verify:

- session
- expiration
- image integrity
- face count
- quality
- liveness
- recognition

The client liveness result is informative, not authoritative.

ANTI-REPLAY

Prepare frame/session contracts for:

- server challenge
- session ID
- sequence number
- timestamp
- expiration

Avoid accepting arbitrary previously captured frames outside the intended session.

TESTING

Create replayable test fixtures so that the same capture can be evaluated by:

Method 1
Method 4
Method 5

Compare:

- verification consistency
- bandwidth
- server CPU
- latency
- number of recognition inferences
- liveness behavior
- behavior under poor-quality frames

SIMULATOR

Allow configuration:

number of frames:
1
2
3
4
5
...

But enforce a server-side maximum.

Show selected frames for development.

Do not implement policy orchestration yet.