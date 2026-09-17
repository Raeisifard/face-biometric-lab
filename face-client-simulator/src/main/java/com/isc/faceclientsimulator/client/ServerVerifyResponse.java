package com.isc.faceclientsimulator.client;

import java.util.List;

/** Canonical biometric verification response shared by all verification modes. */
public record ServerVerifyResponse(
        String requestId,
        String referenceId,
        String captureMethod,
        String result,
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
    public boolean matched() {
        return "MATCH".equals(result);
    }

    public record ModelMetadata(String modelId, String modelVersion, int dimension, String algorithm) {}
    public record QualityLiveness(Double qualityScore, Boolean live, Double livenessScore) {}
    public record Metrics(Long processingTimeMs, Integer decodedFrames, Integer recognitionFrames, Long uploadedBytes) {}
}
