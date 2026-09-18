package com.isc.facebiometricservice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Map;

@ConfigurationProperties(prefix = "biometric.policy")
public record BiometricPolicyProperties(
        boolean enabled,
        String defaultProfile,
        String selectionMode,
        Map<String, PolicyDefinition> profiles,
        Map<String, PolicyDefinition> methodPolicies) {

    public BiometricPolicyProperties {
        defaultProfile = defaultProfile == null || defaultProfile.isBlank() ? "NORMAL" : defaultProfile.toUpperCase();
        selectionMode = selectionMode == null || selectionMode.isBlank() ? "SERVER_ASSIGNED" : selectionMode.toUpperCase();
        profiles = profiles == null ? Map.of() : Map.copyOf(profiles);
        methodPolicies = methodPolicies == null ? Map.of() : Map.copyOf(methodPolicies);
    }

    public record PolicyDefinition(
            String method,
            double minDurationSeconds,
            double maxDurationSeconds,
            int requiredFrameCount,
            double uploadFps,
            long maxPayloadBytes,
            String livenessMode,
            double livenessThreshold,
            double minQualityScore,
            String recognitionModelId,
            String recognitionModelVersion,
            double threshold,
            String fallbackMethod,
            long sessionTtlSeconds,
            long version) {
    }
}
