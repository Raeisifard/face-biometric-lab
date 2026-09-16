package com.isc.facebiometricservice.biometric;

public interface FaceAligner {
    Object align(Object capture, DetectionResult detection);
}
