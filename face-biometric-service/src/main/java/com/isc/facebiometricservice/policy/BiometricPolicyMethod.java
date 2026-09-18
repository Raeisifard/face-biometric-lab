package com.isc.facebiometricservice.policy;

public enum BiometricPolicyMethod {
    SERVER_FULL_CLIP,
    SERVER_LIVE_STREAM,
    CLIENT_EMBEDDING,
    HYBRID_SINGLE_FRAME,
    HYBRID_MULTI_FRAME;

    public static BiometricPolicyMethod parse(String value) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException("POLICY_METHOD_REQUIRED");
        return switch (value.trim().toUpperCase()) {
            case "FULL_CLIP", "SERVER_FULL_CLIP" -> SERVER_FULL_CLIP;
            case "LIVE_STREAM", "SERVER_LIVE_STREAM" -> SERVER_LIVE_STREAM;
            case "CLIENT_EMBEDDING" -> CLIENT_EMBEDDING;
            case "HYBRID_SINGLE_FRAME" -> HYBRID_SINGLE_FRAME;
            case "HYBRID_MULTI_FRAME" -> HYBRID_MULTI_FRAME;
            default -> throw new IllegalArgumentException("UNSUPPORTED_METHOD");
        };
    }

    public String wireValue() {
        return switch (this) {
            case SERVER_FULL_CLIP -> "FULL_CLIP";
            case SERVER_LIVE_STREAM -> "LIVE_STREAM";
            case CLIENT_EMBEDDING -> "CLIENT_EMBEDDING";
            case HYBRID_SINGLE_FRAME -> "HYBRID_SINGLE_FRAME";
            case HYBRID_MULTI_FRAME -> "HYBRID_MULTI_FRAME";
        };
    }
}
