package com.isc.facebiometricservice.model;

import com.isc.facebiometricservice.config.BiometricProperties;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

@Component
public final class FaceModelRegistry {
    public static final String CLIENT_MODEL_ID = "mobilefacenet-512";
    public static final String CLIENT_MODEL_VERSION = "w600k-mbf";

    private final Map<ModelKey, FaceModelDescriptor> models;

    public FaceModelRegistry(BiometricProperties properties) {
        Map<ModelKey, FaceModelDescriptor> registered = new LinkedHashMap<>();

        register(registered, new FaceModelDescriptor(
                properties.modelId(),
                properties.modelVersion(),
                properties.dimension(),
                properties.normalizedRequired(),
                properties.algorithm(),
                "SERVER_VERIFIED"));

        register(registered, new FaceModelDescriptor(
                CLIENT_MODEL_ID,
                CLIENT_MODEL_VERSION,
                properties.dimension(),
                true,
                properties.algorithm(),
                "CLIENT_GENERATED"));

        this.models = Map.copyOf(registered);
    }

    public Optional<FaceModelDescriptor> find(String modelId, String modelVersion) {
        if (modelId == null || modelVersion == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(models.get(new ModelKey(modelId, modelVersion)));
    }

    public boolean supports(String modelId, String modelVersion) {
        return find(modelId, modelVersion).isPresent();
    }

    public record FaceModelDescriptor(
            String modelId,
            String modelVersion,
            int dimension,
            boolean normalized,
            String algorithm,
            String trustBoundary) {
    }

    private record ModelKey(String modelId, String modelVersion) {
    }

    private static void register(Map<ModelKey, FaceModelDescriptor> target, FaceModelDescriptor descriptor) {
        if (descriptor.modelId() == null || descriptor.modelId().isBlank()
                || descriptor.modelVersion() == null || descriptor.modelVersion().isBlank()) {
            throw new IllegalStateException("A registered biometric model must have a model id and version");
        }
        target.put(new ModelKey(descriptor.modelId(), descriptor.modelVersion()), descriptor);
    }
}
