package com.isc.facebiometricservice.streaming;

import java.time.Instant;

public record VerificationSession(
        String sessionId,
        String customerReferenceId,
        Instant startTime,
        Instant expirationTime,
        String expectedCaptureMode,
        String processingState,
        String finalResult
) {
    public VerificationSession {
        if (sessionId == null || sessionId.isBlank()) {
            throw new IllegalArgumentException("sessionId is required");
        }
        if (expectedCaptureMode == null || expectedCaptureMode.isBlank()) {
            expectedCaptureMode = "LIVE_STREAM";
        }
        if (processingState == null || processingState.isBlank()) {
            processingState = "CAPTURING";
        }
    }
}
