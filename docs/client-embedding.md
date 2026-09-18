# Client Embedding

Client Embedding mode is the Method 3 development flow. The simulator performs YuNet detection, quality/liveness analysis, temporal movement checks, alignment, and MobileFaceNet inference locally. It sends a normalized 512-dimensional embedding to the biometric service, which retrieves the matching MobileFaceNet reference and calculates cosine similarity.

## Profiles

| Flow | Model | Profile | Normalization |
| --- | --- | --- | --- |
| Client Embedding | `w600k_mbf.onnx` | `mobilefacenet-512` / `w600k-mbf` | RGB approximately `(pixel - 127.5) / 128.0`, then L2 |
| Full Clip and server video modes | `w600k_r50.onnx` | `arcface-512` / `w600k-r50` | Existing ArcFace preprocessing, then L2 |

These profiles are not interchangeable. A reference enrolled with one profile cannot be compared with a probe from the other profile.

## Model contract

Every embedding verification request must identify its embedding model explicitly:

- `modelId`
- `modelVersion`
- `dimension`

The server resolves this tuple through its model registry. It does **not** infer the model from `dimension=512`, and it never compares a MobileFaceNet probe with an ArcFace/R50 reference.

The server also validates the declared dimension, finite vector values, normalization flag, and L2 norm before reference lookup and cosine comparison.

## Trust boundary

The server verifies dimension, finite values, normalization, model/version, reference compatibility, and cosine similarity. It does not prove that the client actually used the claimed camera, liveness result, or model. The client-reported processing is development evidence only. Production deployment still needs authenticated biometric sessions, a server challenge/nonce, replay prevention, device/app identity, attestation, and binding the embedding to the session.

The simulator endpoints are `/api/v1/simulator/client-embedding`, `/api/v1/simulator/client-enroll`, and `/api/v1/simulator/client-verify`. The browser UI exposes the flow when the server capture policy is `CLIENT_EMBEDDING` or `FREE_METHOD`.

## Reference profiles

A reference is keyed by reference ID **and** model identity. A user can therefore have independent references:

```text
user-123
  ├── mobilefacenet-512 / w600k-mbf
  └── arcface-512 / w600k-r50
```

If the requested model is supported but no compatible reference exists, verification returns `INCONCLUSIVE / REFERENCE_NOT_FOUND`. If a reference exists only for another model, verification returns `INVALID_REQUEST / MODEL_MISMATCH` with HTTP 409.
