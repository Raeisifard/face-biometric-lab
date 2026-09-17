package com.isc.faceclientsimulator.client;

public record ServerEnrollResponse(
        String userId,
        boolean enrolled,
        double similarity,
        double threshold,
        String algorithm,
        String modelId,
        String modelVersion,
        long processingTimeMs
) {}
