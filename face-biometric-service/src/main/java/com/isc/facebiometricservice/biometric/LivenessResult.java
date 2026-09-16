package com.isc.facebiometricservice.biometric;

public record LivenessResult(boolean live, Double score, String reasonCode) {}
