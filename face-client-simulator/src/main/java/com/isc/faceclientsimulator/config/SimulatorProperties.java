package com.isc.faceclientsimulator.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "simulator")
public record SimulatorProperties(
        String serverBaseUrl,
        String modelsDir,
        String detectorModelPath,
        String recognitionModelPath,
        String livenessModelPath,
        String recognitionModelId,
        String recognitionModelVersion,
        float detectionThreshold,
        float matchingLivenessThreshold,
        int livenessMinFrames,
        float requiredTemporalMotion,
        float livenessFrameRate,
        int maxUploadBytes
) {
}
