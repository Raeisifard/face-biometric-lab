package com.isc.facebiometricservice.biometric;

import java.util.Optional;
import com.isc.facebiometricservice.domain.FaceEmbedding;

public interface ReferenceEmbeddingRepository {
    Optional<FaceEmbedding> find(String referenceId, String modelId, String modelVersion);

    /**
     * Finds an enrolled reference without restricting the lookup to a model profile.
     * Implementations that cannot support this query may keep the default empty result.
     */
    default Optional<FaceEmbedding> findByReferenceId(String referenceId) {
        return Optional.empty();
    }
}
