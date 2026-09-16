package com.isc.facebiometricservice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix="biometric.video-verification")
public record VideoVerificationProperties(boolean enabled,long maxClipBytes,double minDurationSeconds,double maxDurationSeconds,
    double sampleFps,int minFaceSizePixels,int maxFaces,double minQualityScore,boolean livenessRequired,
    double livenessThreshold,int recognitionFrames,String aggregation,String detectorModelPath,
    String livenessModelPath,String recognitionModelPath,String storageDirectory) {}
