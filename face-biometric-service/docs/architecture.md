# Architecture

The server is deliberately blind to camera details. The probe vector is an application boundary contract. The client simulator performs the biometric sensing pipeline and then sends only the normalized embedding plus model metadata to this service.

```text
Flutter/client simulator
      -> 512-D normalized embedding
      -> modelId + modelVersion
      -> face-biometric-service
      -> reference repository
      -> cosine similarity
```
