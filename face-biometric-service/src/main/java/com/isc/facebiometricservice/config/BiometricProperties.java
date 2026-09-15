package com.isc.facebiometricservice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Map;

@ConfigurationProperties(prefix = "biometric")
public record BiometricProperties(
        String repositoryType,
        String modelId,
        String modelVersion,
        int dimension,
        double threshold,
        String algorithm,
        boolean normalizedRequired,
        Oracle oracle,
        Mongo mongo,
        Cors cors,
        Map<String, ReferenceEmbedding> referenceEmbeddings
) {

    public record Oracle(
            String url,
            String username,
            String password
    ) {
    }

    public record Mongo(
            String uri,
            String database,
            String collection
    ) {
    }

    public record Cors(
            String allowedOrigins
    ) {
    }

    public record ReferenceEmbedding(
            String modelId,
            String modelVersion,
            int dimension,
            boolean normalized,
            float[] embedding
    ) {
    }
}