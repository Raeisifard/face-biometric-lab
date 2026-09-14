package com.isc.faceclientsimulator.domain;

public record LivenessFrameResult(
        String status,
        boolean frameLooksLive,
        double liveScore,
        double temporalMotion,
        int acceptedFrames,
        int requiredFrames,
        String instruction,
        long processingTimeMs
) {}
