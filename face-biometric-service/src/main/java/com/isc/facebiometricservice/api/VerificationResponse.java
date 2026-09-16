package com.isc.facebiometricservice.api;

import java.util.List;

public record VerificationResponse(
        String requestId,
        String referenceId,
        VerificationStatus result,
        Double similarity,
        Double threshold,
        ModelMetadata model,
        QualityLiveness quality,
        List<String> reasonCodes
) {
    public record ModelMetadata(String modelId, String modelVersion, int dimension, String algorithm) {}
    public record QualityLiveness(Double qualityScore, Boolean live, Double livenessScore) {}
}
