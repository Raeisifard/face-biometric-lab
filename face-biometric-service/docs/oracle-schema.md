# Oracle schema

`FACE_BIOMETRIC_PROFILE` stores one reference embedding per `(userId, modelId, modelVersion)` using a BLOB containing 512 float32 values. Do not use Oracle `SYSTEM` for the application schema in normal deployments.
