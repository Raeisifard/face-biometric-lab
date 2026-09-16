Continue the existing `face-biometric-lab` project.

Phases 00–02 have already been implemented and tested.

Implement ONLY:

METHOD 3 — CLIENT-SIDE FACE RECOGNITION / EMBEDDING

OBJECTIVE

The client performs the majority of biometric processing locally.

The client:

- captures the face
- detects the face
- performs liveness
- performs quality checks
- aligns the face
- executes the face recognition model
- generates a 512-dimensional embedding

The client sends the embedding to the server.

The server retrieves the customer's stored reference embedding and performs the final 1:1 comparison.

FLOW

Client:

Camera
↓
Face Detection
↓
Quality
↓
Liveness
↓
Alignment
↓
Recognition Model
↓
512D embedding
↓
Server

Server:

Receive embedding
↓
Validate dimension
↓
Validate numeric values
↓
Validate normalization expectations
↓
Retrieve reference embedding
↓
Cosine similarity
↓
Threshold decision
↓
Verification result

IMPORTANT SECURITY CONSIDERATION

Do NOT claim that a client-generated embedding is trustworthy merely because it arrived through TLS.

Explicitly document the trust boundary.

The architecture must prepare for:

- authenticated biometric session
- server-issued challenge/nonce
- replay prevention
- device/app identity
- device attestation
- binding embedding to session
- model/version identification

Do not falsely claim that this phase provides strong anti-tampering guarantees.

EMBEDDING CONTRACT

Define a versioned contract containing, as appropriate:

- session ID
- challenge/nonce
- model ID
- model version
- embedding dimension
- normalization flag
- embedding
- client timestamp
- sequence information

Avoid including unnecessary personal data.

SERVER VALIDATION

Reject:

- wrong dimension
- NaN
- Infinity
- malformed values
- unsupported model
- unsupported dimension
- expired session
- invalid challenge
- duplicate submission

Do not blindly trust the client-provided similarity score.

The server must calculate the final similarity itself.

IMPORTANT

The client may report:

- local liveness result
- local quality result
- model version

but the server must distinguish:

CLIENT_REPORTED
from
SERVER_VERIFIED

Do not mix these concepts.

CLIENT SIMULATOR

Implement the client-side biometric pipeline using the existing model abstractions.

The simulator should support:

- webcam capture
- face detection
- liveness
- embedding generation
- visualization of development diagnostics
- submission to server

Do not expose model internals in the production-style API.

TESTING

Test:

1. valid embedding
2. invalid dimension
3. NaN
4. Infinity
5. malformed embedding
6. wrong model version
7. expired session
8. replay
9. successful match
10. no match
11. normalization differences
12. threshold behavior

SECURITY TESTS

Demonstrate that modifying the client embedding changes the result.

Document clearly that this demonstrates the trust limitation rather than solving it.

PERFORMANCE

Measure:

- client inference latency
- client CPU/memory
- embedding size
- network payload
- total verification latency

Compare against Methods 1 and 2.

Do not start Method 4.