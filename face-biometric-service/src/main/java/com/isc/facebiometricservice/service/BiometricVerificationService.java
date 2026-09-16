package com.isc.facebiometricservice.service;

import com.isc.facebiometricservice.api.VerificationRequest;
import com.isc.facebiometricservice.api.VerificationResponse;
import com.isc.facebiometricservice.api.VerificationStatus;
import com.isc.facebiometricservice.biometric.FaceMatcher;
import com.isc.facebiometricservice.biometric.ReferenceEmbeddingRepository;
import com.isc.facebiometricservice.config.BiometricProperties;
import com.isc.facebiometricservice.domain.FaceEmbedding;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;

@Service
public class BiometricVerificationService {
    private final ReferenceEmbeddingRepository repository;
    private final FaceMatcher matcher;
    private final BiometricProperties properties;

    public BiometricVerificationService(ReferenceEmbeddingRepository repository,
                                         FaceMatcher matcher,
                                         BiometricProperties properties) {
        this.repository = repository;
        this.matcher = matcher;
        this.properties = properties;
    }

    public VerificationResponse verify(VerificationRequest request) {
        if (request == null || request.capture() == null) {
            return invalid(null, null, "CAPTURE_REQUIRED");
        }
        if (!"EMBEDDING".equalsIgnoreCase(request.capture().type())) {
            return invalid(request.requestId(), request.referenceId(), "CAPTURE_TYPE_NOT_SUPPORTED");
        }
        if (request.capture().embedding() == null || request.capture().embedding().size() != properties.dimension()) {
            return invalid(request.requestId(), request.referenceId(), "EMBEDDING_DIMENSION_MISMATCH");
        }

        float[] values = new float[request.capture().embedding().size()];
        for (int i = 0; i < values.length; i++) values[i] = request.capture().embedding().get(i);
        boolean normalized = Boolean.TRUE.equals(request.capture().normalized());
        if (properties.normalizedRequired() && !normalized) {
            return invalid(request.requestId(), request.referenceId(), "NORMALIZED_EMBEDDING_REQUIRED");
        }

        var model = request.model();
        String modelId = model != null && model.modelId() != null ? model.modelId() : properties.modelId();
        String modelVersion = model != null && model.modelVersion() != null ? model.modelVersion() : properties.modelVersion();
        if (!properties.modelId().equals(modelId) || !properties.modelVersion().equals(modelVersion)) {
            return invalid(request.requestId(), request.referenceId(), "MODEL_MISMATCH");
        }

        var reference = repository.find(request.referenceId(), modelId, modelVersion);
        if (reference.isEmpty()) {
            return new VerificationResponse(request.requestId(), request.referenceId(), VerificationStatus.INCONCLUSIVE,
                    null, properties.threshold(), metadata(), new VerificationResponse.QualityLiveness(null, null, null),
                    List.of("REFERENCE_NOT_FOUND"));
        }
        FaceEmbedding ref = reference.get();
        try {
            double similarity = matcher.similarity(values, ref.values());
            VerificationStatus result = similarity >= properties.threshold()
                    ? VerificationStatus.MATCH : VerificationStatus.NO_MATCH;
            return new VerificationResponse(request.requestId(), request.referenceId(), result, similarity,
                    properties.threshold(), metadata(), new VerificationResponse.QualityLiveness(null, null, null),
                    List.of(result == VerificationStatus.MATCH ? "SIMILARITY_ABOVE_THRESHOLD" : "SIMILARITY_BELOW_THRESHOLD"));
        } catch (IllegalArgumentException ex) {
            return new VerificationResponse(request.requestId(), request.referenceId(), VerificationStatus.INVALID_REQUEST,
                    null, properties.threshold(), metadata(), new VerificationResponse.QualityLiveness(null, null, null),
                    List.of("INVALID_EMBEDDING"));
        }
    }

    private VerificationResponse invalid(String requestId, String referenceId, String reason) {
        return new VerificationResponse(requestId, referenceId, VerificationStatus.INVALID_REQUEST, null,
                properties.threshold(), metadata(), new VerificationResponse.QualityLiveness(null, null, null), List.of(reason));
    }

    private VerificationResponse.ModelMetadata metadata() {
        return new VerificationResponse.ModelMetadata(properties.modelId(), properties.modelVersion(),
                properties.dimension(), matcher.algorithm());
    }
}
