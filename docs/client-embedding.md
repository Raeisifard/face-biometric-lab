# Client Embedding

Client Embedding mode is the Method 3 development flow. The simulator performs YuNet detection, quality/liveness analysis, temporal movement checks, alignment, and MobileFaceNet inference locally. It sends a normalized 512-dimensional embedding to the biometric service, which retrieves the matching MobileFaceNet reference and calculates cosine similarity.

## Profiles

| Flow | Model | Profile | Normalization |
| --- | --- | --- | --- |
| Client Embedding | `w600k_mbf.onnx` | `mobilefacenet-512` / `w600k-mbf` | RGB approximately `(pixel - 127.5) / 128.0`, then L2 |
| Full Clip and server video modes | `w600k_r50.onnx` | `arcface-512` / `w600k-r50` | Existing ArcFace preprocessing, then L2 |

These profiles are not interchangeable. A reference enrolled with one profile cannot be compared with a probe from the other profile.

## Trust boundary

The server verifies dimension, finite values, normalization, model/version, and cosine similarity. It does not prove that the client actually used the claimed camera, liveness result, or model. The client-reported processing is development evidence only. Production deployment still needs authenticated biometric sessions, a server challenge/nonce, replay prevention, device/app identity, attestation, and binding the embedding to the session.

The simulator endpoints are `/api/v1/simulator/client-embedding`, `/api/v1/simulator/client-enroll`, and `/api/v1/simulator/client-verify`. The browser UI exposes the flow when the server capture policy is `CLIENT_EMBEDDING` or `FREE_METHOD`.