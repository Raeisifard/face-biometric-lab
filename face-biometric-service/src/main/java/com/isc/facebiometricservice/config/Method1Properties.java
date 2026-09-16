package com.isc.facebiometricservice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix="biometric.method1")
public record Method1Properties(boolean enabled, long maxClipBytes, double minDurationSeconds, double maxDurationSeconds,
    double sampleFps, int minFaceSizePixels, int maxFaces, double minQualityScore, boolean livenessRequired,
    double livenessThreshold, int recognitionFrames, String aggregation, String detectorModelPath,
    String livenessModelPath, String recognitionModelPath) {}
