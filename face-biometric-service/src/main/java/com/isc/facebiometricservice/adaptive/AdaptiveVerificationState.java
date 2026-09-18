package com.isc.facebiometricservice.adaptive;

public enum AdaptiveVerificationState {
    CREATED,
    POLICY_ASSIGNED,
    CAPTURING,
    PROCESSING,
    EVALUATED,
    ESCALATING,
    COMPLETED,
    EXPIRED,
    FAILED
}
