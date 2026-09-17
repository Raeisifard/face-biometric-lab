package com.isc.facebiometricservice.service;

import com.isc.facebiometricservice.biometric.FaceMatcher;
import com.isc.facebiometricservice.config.BiometricProperties;
import com.isc.facebiometricservice.domain.FaceEmbedding;
import com.isc.facebiometricservice.domain.SimilarityResult;
import com.isc.facebiometricservice.repository.FaceEmbeddingRepository;
import org.springframework.stereotype.Service;

@Service
public class FaceMatchingService {
    public static final String CLIENT_MODEL_ID = "mobilefacenet-512";
    public static final String CLIENT_MODEL_VERSION = "w600k-mbf";
    private final FaceEmbeddingRepository repository;
    private final BiometricProperties properties;
    private final FaceMatcher matcher;

    public FaceMatchingService(FaceEmbeddingRepository repository, BiometricProperties properties, FaceMatcher matcher) {
        this.repository = repository;
        this.properties = properties;
        this.matcher = matcher;
    }

    public void enroll(String userId, FaceEmbedding embedding) {
        validate(embedding);
        repository.save(userId, embedding);
    }

    public SimilarityResult verify(String userId, FaceEmbedding probe) {
        long started = System.nanoTime();
        validate(probe);
        FaceEmbedding ref = repository.find(userId, probe.modelId(), probe.modelVersion())
                .orElseThrow(() -> new IllegalArgumentException("Reference embedding not found"));
        double similarity = matcher.similarity(probe.values(), ref.values());
        double threshold = properties.threshold();
        return new SimilarityResult(similarity, threshold, similarity >= threshold,
                matcher.algorithm(), (System.nanoTime() - started) / 1_000_000);
    }

    private void validate(FaceEmbedding e) {
        if (e == null || e.values() == null || e.dimension() != properties.dimension()
                || e.values().length != properties.dimension()) {
            throw new IllegalArgumentException("Embedding dimension mismatch");
        }
        if (!supportsModel(e.modelId(), e.modelVersion()))
            throw new IllegalArgumentException("Model/version mismatch");
        if (properties.normalizedRequired() && !e.normalized())
            throw new IllegalArgumentException("Normalized embedding is required");
        for (float value : e.values()) {
            if (!Float.isFinite(value)) throw new IllegalArgumentException("Embedding contains non-finite values");
        }
    }

    public boolean supportsModel(String modelId, String modelVersion) {
        return properties.modelId().equals(modelId) && properties.modelVersion().equals(modelVersion)
                || CLIENT_MODEL_ID.equals(modelId) && CLIENT_MODEL_VERSION.equals(modelVersion);
    }
}
