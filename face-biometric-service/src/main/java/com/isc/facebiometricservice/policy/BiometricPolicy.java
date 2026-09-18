package com.isc.facebiometricservice.policy;

import java.time.Instant;

public record BiometricPolicy(
        String policyId,
        long version,
        String profile,
        BiometricPolicyMethod method,
        CaptureRequirements capture,
        LivenessRequirements liveness,
        double minQualityScore,
        RecognitionRequirements recognition,
        String fallbackMethod,
        long sessionTtlSeconds,
        Instant issuedAt) {

    public record CaptureRequirements(double minDurationSeconds, double maxDurationSeconds,
                                      int requiredFrameCount, double uploadFps, long maxPayloadBytes) {}
    public record LivenessRequirements(String mode, boolean required, double threshold) {}
    public record RecognitionRequirements(String modelId, String modelVersion, double threshold) {}
}
