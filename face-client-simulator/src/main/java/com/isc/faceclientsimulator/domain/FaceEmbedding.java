package com.isc.faceclientsimulator.domain;

import java.util.Arrays;

public record FaceEmbedding(
        float[] values,
        int dimension,
        String modelId,
        String modelVersion,
        boolean normalized
) {
    public FaceEmbedding {
        if (values == null || values.length != 512) throw new IllegalArgumentException("Face embedding must contain exactly 512 values");
        if (dimension != 512) throw new IllegalArgumentException("Embedding dimension must be 512");
        values = Arrays.copyOf(values, values.length);
    }
    @Override public float[] values() { return Arrays.copyOf(values, values.length); }
}
