package com.isc.facebiometricservice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "biometric.hybrid-multi-frame")
public record HybridMultiFrameProperties(
        boolean enabled,
        int defaultFrames,
        int maxFrames,
        long maxFrameBytes,
        long sessionTtlSeconds,
        String livenessMode,
        String aggregation
) {
    public HybridMultiFrameProperties {
        defaultFrames = defaultFrames <= 0 ? 4 : defaultFrames;
        maxFrames = maxFrames <= 0 ? 8 : maxFrames;
        maxFrameBytes = maxFrameBytes <= 0 ? 8L * 1024 * 1024 : maxFrameBytes;
        sessionTtlSeconds = sessionTtlSeconds <= 0 ? 120 : sessionTtlSeconds;
        livenessMode = livenessMode == null || livenessMode.isBlank() ? "PASSIVE" : livenessMode;
        aggregation = aggregation == null || aggregation.isBlank() ? "MEAN" : aggregation;
    }
}
