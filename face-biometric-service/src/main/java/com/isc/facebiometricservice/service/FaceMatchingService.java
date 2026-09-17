package com.isc.facebiometricservice.service;

import com.isc.facebiometricservice.biometric.FaceMatcher;
import com.isc.facebiometricservice.config.BiometricProperties;
import com.isc.facebiometricservice.domain.FaceEmbedding;
import com.isc.facebiometricservice.domain.SimilarityResult;
import com.isc.facebiometricservice.repository.FaceEmbeddingRepository;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class FaceMatchingService {
    private static final Logger log = LoggerFactory.getLogger(FaceMatchingService.class);
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
        log.info("[SERVER_EMBEDDING] enroll start user={} model={} version={} dimension={} normalized={}", userId, embedding == null ? null : embedding.modelId(), embedding == null ? null : embedding.modelVersion(), embedding == null ? null : embedding.dimension(), embedding != null && embedding.normalized());
        validate(embedding);
        repository.save(userId, embedding);
        log.info("[SERVER_EMBEDDING] enroll accepted user={} model={} version={}", userId, embedding.modelId(), embedding.modelVersion());
    }

    public SimilarityResult verify(String userId, FaceEmbedding probe) {
        long started = System.nanoTime();
        log.info("[SERVER_EMBEDDING] verify start user={} model={} version={} dimension={} normalized={}", userId, probe == null ? null : probe.modelId(), probe == null ? null : probe.modelVersion(), probe == null ? null : probe.dimension(), probe != null && probe.normalized());
        validate(probe);
        FaceEmbedding ref = repository.find(userId, probe.modelId(), probe.modelVersion())
                .orElseThrow(() -> new IllegalArgumentException("Reference embedding not found"));
        log.info("[SERVER_EMBEDDING] reference loaded user={} model={} version={} dimension={}", userId, ref.modelId(), ref.modelVersion(), ref.dimension());
        double similarity = matcher.similarity(probe.values(), ref.values());
        double threshold = properties.threshold();
        SimilarityResult result = new SimilarityResult(similarity, threshold, similarity >= threshold,
                matcher.algorithm(), (System.nanoTime() - started) / 1_000_000);
        log.info("[SERVER_EMBEDDING] verify decision user={} similarity={} threshold={} matched={} algorithm={} processingMs={}", userId, result.similarity(), result.threshold(), result.matched(), result.algorithm(), result.processingTimeMs());
        return result;
    }

    private void validate(FaceEmbedding e) {
        log.debug("[SERVER_EMBEDDING] validate model={} version={} dimension={} values={} normalized={}", e == null ? null : e.modelId(), e == null ? null : e.modelVersion(), e == null ? null : e.dimension(), e == null || e.values() == null ? null : e.values().length, e != null && e.normalized());
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
