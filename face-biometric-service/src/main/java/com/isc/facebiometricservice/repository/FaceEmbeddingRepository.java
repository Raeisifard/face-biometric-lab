package com.isc.facebiometricservice.repository;

import com.isc.facebiometricservice.domain.FaceEmbedding;
import java.util.Optional;

public interface FaceEmbeddingRepository {
    void save(String userId, FaceEmbedding embedding);
    Optional<FaceEmbedding> find(String userId, String modelId, String modelVersion);
}
