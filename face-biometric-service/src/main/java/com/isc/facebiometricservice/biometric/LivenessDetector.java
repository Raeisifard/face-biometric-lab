package com.isc.facebiometricservice.biometric;

public interface LivenessDetector {
    LivenessResult detect(Object capture, DetectionResult detection);
}
