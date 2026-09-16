package com.isc.facebiometricservice.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

public record VerificationRequest(
        @NotBlank String requestId,
        @NotBlank String referenceId,
        @Valid CaptureData capture,
        ModelMetadata model,
        String nonce,
        String sessionId
) {
    public record CaptureData(
            @NotBlank String type,
            java.util.List<Float> embedding,
            Boolean normalized
    ) {}
    public record ModelMetadata(String modelId, String modelVersion, Integer dimension) {}
}
