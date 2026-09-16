package com.isc.facebiometricservice.biometric;

public interface FaceQualityAnalyzer {
    QualityResult analyze(Object capture, DetectionResult detection);
}
