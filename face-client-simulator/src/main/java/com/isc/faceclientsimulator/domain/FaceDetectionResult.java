package com.isc.faceclientsimulator.domain;

public record FaceDetectionResult(
        boolean faceDetected,
        int faceCount,
        FaceBoundingBox box,
        FaceLandmarks landmarks,
        double confidence,
        long processingTimeMs
) {
    public static FaceDetectionResult noFace(long timeMs, int faceCount) {
        return new FaceDetectionResult(false, faceCount, null, null, 0.0, timeMs);
    }
}
