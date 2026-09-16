package com.isc.facebiometricservice.biometric;

public interface FaceMatcher {
    double similarity(float[] probe, float[] reference);
    String algorithm();
}
