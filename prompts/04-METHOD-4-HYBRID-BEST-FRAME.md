Continue the existing `face-biometric-lab`.

Phases 00–03 have been implemented and evaluated.

Implement ONLY:

METHOD 4 — HYBRID SINGLE BEST FRAME

OBJECTIVE

The client performs lightweight preprocessing:

- face detection
- face tracking if available
- basic quality assessment
- liveness
- best-frame selection

The client does NOT perform final recognition.

The client sends one selected high-quality face image to the server.

The server independently performs:

- face detection
- face count validation
- face quality validation
- liveness validation as configured
- alignment
- ArcFace / w600k-r50 recognition
- reference lookup
- cosine comparison
- final decision

FLOW

CLIENT

Camera
↓
Detection
↓
Quality
↓
Liveness
↓
Best Frame Selection
↓
ONE IMAGE
↓
SERVER

SERVER

Receive image
↓
Decode
↓
Detect
↓
Validate exactly one face
↓
Quality
↓
Liveness
↓
Alignment
↓
W600K-R50
↓
512D embedding
↓
Reference embedding
↓
Cosine similarity
↓
Final result

CRITICAL SECURITY PROPERTY

Do not trust the client merely because it says:

liveness=true

The server must independently perform the required security checks.

The client-side liveness result is an optimization/input signal, not authoritative proof.

BEST FRAME SELECTION

Implement a configurable scoring mechanism based on available quality signals such as:

- face size
- sharpness
- brightness
- pose
- occlusion
- detector confidence

Do not hard-code arbitrary production thresholds without documenting them.

The selected frame must be traceable to the verification session.

SERVER VALIDATION

Server must reject:

- multiple faces
- no face
- poor quality
- invalid image
- suspicious dimensions
- excessive image size
- failed liveness
- expired session
- replayed submission

TESTING

Compare Method 4 against Method 1 using the same source video/frames.

Measure:

- bandwidth
- server processing time
- client processing time
- total latency
- selected-frame quality
- verification result consistency

Build a test mode that can replay the same recorded capture through:

Method 1
and
Method 4

This will allow objective comparison.

SIMULATOR

Show:

- live webcam
- detected face
- quality indicators
- liveness state
- selected frame
- upload state
- final server result

Do not send the complete video.

Do not implement Method 5 yet.