Continue development of the existing `face-biometric-lab` project.

Phase 00 has already been implemented and tested.

Implement ONLY:

METHOD 1 — FULL VIDEO CLIP SENT TO SERVER

Do not implement Methods 2–5.
Do not implement the policy engine.
Do not implement adaptive fallback.

OBJECTIVE

The client simulator captures a short face video/clip, approximately 3–5 seconds.

The complete clip is sent to the face-biometric-service.

The server performs the biometric pipeline.

TARGET FLOW

Client:

Camera
↓
Capture 3–5 second clip
↓
Upload clip
↓
Server

Server:

Receive clip
↓
Decode frames
↓
Face detection
↓
Face count validation
↓
Face tracking/consistency if appropriate
↓
Face quality analysis
↓
Liveness analysis
↓
Select suitable frames
↓
Face alignment
↓
ArcFace / w600k-r50 embedding
↓
Compare against customer's reference embedding
↓
Produce 1:1 verification result

IMPORTANT

The server must NOT simply use the first frame.

It should inspect the clip and select suitable frames according to configurable quality criteria.

The system should support configurable:

- maximum clip duration
- minimum clip duration
- maximum upload size
- frame sampling rate
- minimum face size
- maximum number of faces
- minimum acceptable quality
- liveness requirements
- number of recognition frames
- aggregation strategy

The server must reject or mark INCONCLUSIVE when:

- no face exists
- multiple faces exist when exactly one is required
- face is too small
- image/frame quality is insufficient
- liveness fails
- clip is invalid
- clip is too long
- clip exceeds configured size
- required processing cannot be completed

RESULT

Return a structured 1:1 verification result.

Do not expose raw internal exceptions.

Distinguish:

MATCH
NO_MATCH
INCONCLUSIVE
INVALID_REQUEST
PROCESSING_ERROR

For diagnostics, provide reason codes such as:

NO_FACE
MULTIPLE_FACES
LOW_LIGHT
BLUR
FACE_TOO_SMALL
BAD_POSE
LIVENESS_FAILED
INVALID_VIDEO
VIDEO_TOO_LONG
VIDEO_TOO_LARGE
MODEL_ERROR

Do not expose sensitive implementation details to the client.

SIMILARITY

Support cosine similarity through the existing FaceMatcher abstraction.

Do not claim that the existing threshold is production/banking-grade.

Make threshold configurable.

TESTING

Create unit and integration tests for:

1. valid clip
2. invalid clip
3. no face
4. multiple faces
5. low quality
6. liveness failure
7. successful verification
8. failed verification
9. oversized clip
10. excessive duration
11. malformed video
12. multiple candidate frames
13. frame selection
14. score aggregation

If real video fixtures are not available, create test infrastructure that can later consume real fixtures.

Do not generate fake biometric accuracy claims.

SIMULATOR

Add a UI flow that allows:

- selecting/capturing a webcam
- viewing the captured clip
- configuring capture duration
- submitting the clip
- displaying server processing state
- displaying final verification result
- displaying non-sensitive diagnostic information

The simulator must remain Spring Boot based and must not require Node.js.

PERFORMANCE

Do not prematurely optimize.

Measure:

- upload size
- upload time
- server processing time
- number of decoded frames
- number of frames processed by recognition
- total verification latency

DELIVERABLES

Provide:

1. implementation
2. tests
3. REST API documentation
4. simulator UI changes
5. configuration examples
6. manual test procedure
7. performance measurements
8. known limitations

Do not start Method 2.