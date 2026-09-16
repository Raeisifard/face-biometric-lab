package com.isc.facebiometricservice.biometric;

public record QualityResult(boolean acceptable, Double score, String reasonCode) {}
