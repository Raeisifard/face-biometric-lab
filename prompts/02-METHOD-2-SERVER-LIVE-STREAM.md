Continue the existing `face-biometric-lab` project.

Phase 01 is complete and has been tested.

Implement ONLY:

METHOD 2 — LIVE/STREAMING FACE CAPTURE TO SERVER

Do not implement Methods 3–5.
Do not implement the policy engine.
Do not implement adaptive orchestration.

OBJECTIVE

Instead of uploading a complete 3–5 second video file, the client continuously sends camera frames to the server during capture.

The server processes frames incrementally and can send feedback to the client before final verification.

TARGET FLOW

Client:

Camera
↓
Frame capture
↓
Frame sampling/compression
↓
Streaming transport
↓
Server

Server:

Receive frame
↓
Decode
↓
Face detection
↓
Face count
↓
Quality
↓
Liveness/temporal analysis
↓
Progressive state
↓
Client feedback
↓
Enough evidence?
↓
Recognition
↓
Final verification

IMPORTANT ARCHITECTURAL REQUIREMENT

Do not blindly send the camera's native FPS.

The upload frame rate must be configurable.

For example:

camera FPS: 30
upload FPS: 5

Support configurable/adaptive frame sampling.

The system should be designed so that transport can later be replaced without changing biometric processing interfaces.

Prefer a clean streaming/session abstraction.

A verification session should have:

- session ID
- customer/reference ID
- start time
- expiration
- expected capture mode
- current processing state
- final result

SERVER FEEDBACK

The server must be able to communicate actionable feedback such as:

NO_FACE
MULTIPLE_FACES
MOVE_CLOSER
MOVE_FARTHER
FACE_NOT_CENTERED
LOW_LIGHT
BLUR
BAD_POSE
GOOD_FRAME
LIVENESS_PROGRESS
CAPTURE_CONTINUE
CAPTURE_COMPLETE
VERIFICATION_SUCCESS
VERIFICATION_FAILED

Do not make feedback depend on UI-specific strings.

Use machine-readable reason/status codes.

LIVENESS

The streaming architecture should support temporal liveness.

Do not claim that streaming automatically guarantees anti-spoofing.

Make liveness implementation replaceable behind the existing interface.

The server must be able to evaluate evidence over multiple frames.

RECOGNITION

Do not run the heavy recognition model unnecessarily on every incoming frame.

Introduce a configurable recognition strategy.

For example:

- process every Nth suitable frame
- process only high-quality frames
- stop after enough evidence
- process a configurable number of frames

The final score should be aggregated using a configurable strategy.

TRANSPORT

Choose a transport appropriate for browser/client-to-Spring communication.

Do not introduce WebRTC unless there is a clear reason.

Keep the architecture modular enough that WebSocket or another streaming transport can be substituted later.

SECURITY

Bind every frame to a verification session.

Prevent frames from being submitted after session expiration.

Prepare the design for nonce/challenge binding and authenticated sessions.

Do not implement an unsafe anonymous production biometric endpoint.

SIMULATOR

The simulator must:

- show webcam video
- start/stop a biometric session
- stream frames
- display live server feedback
- display processing state
- display final result
- show latency and frame counters for development
- allow configurable upload FPS

TESTING

Test:

1. session creation
2. session expiration
3. frame upload
4. invalid frame
5. no face
6. multiple faces
7. quality feedback
8. liveness progression
9. recognition triggering
10. session completion
11. duplicate/late frames
12. high frame rate behavior
13. disconnect/reconnect
14. final verification

PERFORMANCE

Measure:

- frames/sec uploaded
- average frame size
- bandwidth/sec
- server processing latency
- recognition invocations
- end-to-end latency
- CPU/memory usage

Compare these measurements with Method 1.

Do not start Method 3.