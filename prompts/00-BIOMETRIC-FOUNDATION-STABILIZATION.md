You are working on my existing Java 21 / Spring Boot face biometric project named `face-biometric-lab`.

The project currently contains two Maven modules:

1. face-biometric-service
2. face-client-simulator

Do NOT redesign the project from scratch.

Your first task is to establish a stable common foundation that all subsequent biometric verification methods will use.

IMPORTANT:
This is Phase 00 only.

Do NOT implement the five biometric capture methods yet.
Do NOT implement streaming.
Do NOT implement client-side recognition.
Do NOT implement the policy engine.
Do NOT implement adaptive/fallback orchestration.

The purpose of this phase is to stabilize the common contracts, model interfaces, configuration, testing infrastructure, and simulator/server boundaries so that later phases can be implemented independently.

TARGET TECHNOLOGY

- Java 21
- Spring Boot 3.x
- Maven
- REST
- OpenCV where already used
- ONNX Runtime where already used
- Log4j2 if logging is required
- No Node.js frontend build
- The simulator should remain usable through its Spring Boot web UI.
- Keep MongoDB and Oracle optional/disabled by default.
- Do not introduce unnecessary external services.

CURRENT BIOMETRIC CONCEPT

The system performs 1:1 face verification.

A known customer has a stored reference face/template.

A live capture from the client is compared with that customer's reference.

The final result must distinguish at least:

- MATCH
- NO_MATCH
- INCONCLUSIVE
- INVALID_REQUEST
- PROCESSING_ERROR

Create or stabilize clear interfaces for:

- FaceDetector
- FaceAligner
- FaceQualityAnalyzer
- LivenessDetector
- FaceEmbeddingModel
- FaceMatcher
- ReferenceEmbeddingRepository
- BiometricVerificationService

Do not force implementations into these interfaces if the existing project already has equivalent abstractions. Reuse and refactor existing abstractions instead.

The embedding model must remain configurable.

The current intended recognition model is ArcFace / w600k-r50 producing a 512-dimensional embedding.

Do not hard-code model paths.

Configuration must support:

- model path
- model version
- embedding dimension
- similarity algorithm
- threshold
- normalization requirement
- detector configuration
- liveness configuration

REFERENCE DATA

The project currently supports in-memory reference embeddings through configuration.

Keep this functionality.

MongoDB and Oracle must remain optional and must not be required for startup.

TESTING

Create a proper test strategy for:

1. DTO validation
2. embedding dimension validation
3. normalization validation
4. cosine similarity
5. threshold behavior
6. invalid image handling
7. missing face
8. multiple faces
9. repository lookup
10. service-level verification result mapping

Do not invent biometric accuracy claims.

Do not claim a threshold is suitable for banking merely because tests pass.

The tests should make it easy to replace the synthetic/reference data with a real biometric evaluation dataset later.

API

Create/stabilize a clean REST contract for 1:1 verification.

The contract should identify:

- verification session/request ID
- customer/reference identity
- capture data
- algorithm/model metadata where appropriate
- verification result
- similarity score where appropriate
- quality/liveness information
- reason codes

Do not expose internal ONNX/OpenCV implementation details unnecessarily.

SECURITY

Prepare the architecture so later phases can add:

- request nonce/challenge
- session binding
- replay protection
- client identity
- device attestation
- authentication

Do not implement all of these in this phase unless they already exist.

DELIVERABLES

At the end:

1. Show the final module structure.
2. Show important interfaces.
3. Show DTOs.
4. Show configuration.
5. Show REST endpoints.
6. Show tests.
7. Explain how to start both modules.
8. Explain how to verify the foundation manually.
9. List all assumptions.
10. List anything deliberately deferred to later phases.

Most importantly:

Do not modify architecture merely for theoretical future requirements.

Keep this phase small, stable, and executable.