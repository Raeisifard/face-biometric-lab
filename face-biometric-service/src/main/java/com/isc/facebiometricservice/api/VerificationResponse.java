package com.isc.facebiometricservice.api;

import java.util.List;

/**
 * Canonical response contract for every biometric verification mode.
 *
 * Clients should make decisions from {@code result} and {@code code};
 * mode-specific endpoints only populate the metrics that are available.
 */
public record VerificationResponse(
        String requestId,
        String referenceId,
        String captureMethod,
        VerificationStatus result,
        String code,
        String message,
        Integer httpStatus,
        Double similarity,
        Double threshold,
        ModelMetadata model,
        QualityLiveness quality,
        Metrics metrics,
        List<String> reasonCodes
) {
    public record ModelMetadata(String modelId, String modelVersion, int dimension, String algorithm) {}

    public record QualityLiveness(
            Double qualityScore,
            Boolean live,
            Double livenessScore
    ) {}

    public record Metrics(
            Long processingTimeMs,
            Integer decodedFrames,
            Integer recognitionFrames,
            Long uploadedBytes
    ) {}
}
