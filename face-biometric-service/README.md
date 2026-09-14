# face-biometric-service

Independent server-side face verification service. It intentionally does not perform webcam access, face detection or face recognition inference. It accepts a 512-dimensional probe embedding from the client simulator (representing a future Flutter mobile client), retrieves the authoritative reference embedding and compares the two vectors.

## API

`POST /api/v1/biometric/enroll-embedding`

```json
{
  "userId":"user-123",
  "embedding":[0.001, ... 512 values ...],
  "dimension":512,
  "modelId":"arcface-512",
  "modelVersion":"w600k-r50",
  "normalized":true
}
```

`POST /api/v1/biometric/verify-embedding`

Same payload; the service returns similarity, threshold, algorithm and match decision.

`GET /api/v1/biometric/models`

## Repository modes

* `memory` — default, easiest for development.
* `oracle` — recommended authoritative store for your banking architecture.
* `mongo` — convenient for experiments and benchmark datasets.

## Oracle

Use a dedicated application schema rather than Oracle `SYSTEM` for normal development. The included Flyway migration creates `FACE_BIOMETRIC_PROFILE` with a binary float32 embedding BLOB. This is a 1:1 verification store, not an identification/vector-search design.

Run with:

```powershell
$env:SPRING_PROFILES_ACTIVE="oracle"
.\mvnw.cmd -pl face-biometric-service spring-boot:run
```

If the target Oracle schema is already non-empty and contains no Flyway history, do not blindly enable baseline-on-migrate in a shared schema. Prefer a dedicated schema, or set the baseline deliberately after inspecting the database.

## Model contract

The service rejects:

* any vector that is not exactly 512-D
* model ID/version mismatches
* non-normalized embeddings when normalization is required
* zero vectors

This prevents accidental comparison of vectors from different model spaces.
