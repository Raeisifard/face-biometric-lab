package com.isc.facebiometricservice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix="biometric")
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
        Cors cors
) {
    public record Oracle(String url,String username,String password){}
    public record Mongo(String uri,String database,String collection){}
    public record Cors(String allowedOrigins){}
}
