package com.isc.facebiometricservice.biometric;

public interface FaceEmbeddingModel {
    EmbeddingResult embed(Object alignedFace);
    String modelId();
    String modelVersion();
    int dimension();
}
