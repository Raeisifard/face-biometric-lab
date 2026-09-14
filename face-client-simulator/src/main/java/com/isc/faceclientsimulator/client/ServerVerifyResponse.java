package com.isc.faceclientsimulator.client;

public record ServerVerifyResponse(
        String userId,
        boolean matched,
        double similarity,
        double threshold,
        String algorithm,
        String modelId,
        String modelVersion,
        long processingTimeMs
) {}
