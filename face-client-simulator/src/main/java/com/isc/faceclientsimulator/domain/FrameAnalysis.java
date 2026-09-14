package com.isc.faceclientsimulator.domain;

public record FrameAnalysis(
        String sessionId,
        FaceDetectionResult detection,
        LivenessFrameResult liveness,
        boolean embeddingReady
) {}
