package com.isc.facebiometricservice.streaming;

import java.time.Instant;

public record FrameUploadResult(
        String sessionId,
        String feedbackCode,
        String state,
        String message,
        long frameSizeBytes,
        Instant receivedAt
) {
}
