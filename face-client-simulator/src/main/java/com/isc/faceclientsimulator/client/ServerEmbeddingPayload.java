package com.isc.faceclientsimulator.client;

public record ServerEmbeddingPayload(
        String userId,
        float[] embedding,
        int dimension,
        String modelId,
        String modelVersion,
        boolean normalized
) {}
