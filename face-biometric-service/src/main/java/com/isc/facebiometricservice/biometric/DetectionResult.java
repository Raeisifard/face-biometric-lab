package com.isc.facebiometricservice.biometric;

public record DetectionResult(int faceCount, Object primaryFace) {
    public static DetectionResult none() { return new DetectionResult(0, null); }
    public boolean hasExactlyOneFace() { return faceCount == 1 && primaryFace != null; }
}
