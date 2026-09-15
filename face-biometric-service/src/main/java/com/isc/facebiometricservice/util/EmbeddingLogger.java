package com.isc.facebiometricservice.util;

import org.slf4j.Logger;

public final class EmbeddingLogger {

    private EmbeddingLogger() {
    }

    public static void logYaml(
            Logger log,
            String userId,
            String modelId,
            String modelVersion,
            int dimension,
            boolean normalized,
            float[] embedding) {

        log.info(
                "[BIOMETRIC] REFERENCE EMBEDDING - COPY INTO biometric-embeddings.yml\n" +
                        "{}:\n" +
                        "  model-id: {}\n" +
                        "  model-version: {}\n" +
                        "  dimension: {}\n" +
                        "  normalized: {}\n" +
                        "  embedding:",
                userId,
                modelId,
                modelVersion,
                dimension,
                normalized
        );

        for (float value : embedding) {
            log.info("    - {}", value);
        }
    }
}