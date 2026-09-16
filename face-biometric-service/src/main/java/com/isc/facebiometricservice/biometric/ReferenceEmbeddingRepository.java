package com.isc.facebiometricservice.biometric;

import java.util.Optional;
import com.isc.facebiometricservice.domain.FaceEmbedding;

public interface ReferenceEmbeddingRepository {
    Optional<FaceEmbedding> find(String referenceId, String modelId, String modelVersion);
}
