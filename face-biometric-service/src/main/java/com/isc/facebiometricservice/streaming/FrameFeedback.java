package com.isc.facebiometricservice.streaming;

public record FrameFeedback(String code, String message, String state) {
    public FrameFeedback {
        if (code == null || code.isBlank()) {
            code = "GOOD_FRAME";
        }
        if (state == null || state.isBlank()) {
            state = "CAPTURING";
        }
    }
}
