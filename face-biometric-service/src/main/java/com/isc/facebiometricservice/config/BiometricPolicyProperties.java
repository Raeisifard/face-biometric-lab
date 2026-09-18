package com.isc.facebiometricservice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Map;

@ConfigurationProperties(prefix = "biometric.policy")
public record BiometricPolicyProperties(
        boolean enabled,
        String defaultProfile,
        Map<String, PolicyDefinition> profiles) {

    public BiometricPolicyProperties {
        defaultProfile = defaultProfile == null || defaultProfile.isBlank() ? "NORMAL" : defaultProfile.toUpperCase();
        profiles = profiles == null ? Map.of() : Map.copyOf(profiles);
    }

    public record PolicyDefinition(
            String method,
            double minDurationSeconds,
            double maxDurationSeconds,
            int requiredFrameCount,
            double uploadFps,
            long maxPayloadBytes,
            String livenessMode,
            double minQualityScore,
            String recognitionModelId,
            String recognitionModelVersion,
            double threshold,
            String fallbackMethod,
            long sessionTtlSeconds,
            long version) {
    }
}
