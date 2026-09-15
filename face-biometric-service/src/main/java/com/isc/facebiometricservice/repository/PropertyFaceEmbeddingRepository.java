package com.isc.facebiometricservice.repository;

import com.isc.facebiometricservice.config.BiometricProperties;
import com.isc.facebiometricservice.domain.FaceEmbedding;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@ConditionalOnProperty(
        name = "biometric.repository-type",
        havingValue = "property"
)
public class PropertyFaceEmbeddingRepository implements FaceEmbeddingRepository {

    private final BiometricProperties properties;

    public PropertyFaceEmbeddingRepository(BiometricProperties properties) {
        this.properties = properties;
    }

    @Override
    public void save(String userId, FaceEmbedding embedding) {
        throw new UnsupportedOperationException(
                "Enrollment is disabled when biometric.repository-type=property"
        );
    }

    @Override
    public Optional<FaceEmbedding> find(
            String userId,
            String modelId,
            String modelVersion
    ) {
        if (properties.referenceEmbeddings() == null) {
            return Optional.empty();
        }

        BiometricProperties.ReferenceEmbedding reference =
                properties.referenceEmbeddings().get(userId);

        if (reference == null) {
            return Optional.empty();
        }

        if (!modelId.equals(reference.modelId())
                || !modelVersion.equals(reference.modelVersion())) {
            return Optional.empty();
        }

        return Optional.of(
                new FaceEmbedding(
                        reference.embedding(),
                        reference.dimension(),
                        reference.modelId(),
                        reference.modelVersion(),
                        reference.normalized()
                )
        );
    }
}