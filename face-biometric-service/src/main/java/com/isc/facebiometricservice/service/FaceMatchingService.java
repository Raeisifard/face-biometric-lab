package com.isc.facebiometricservice.service;

import com.isc.facebiometricservice.biometric.FaceMatcher;
import com.isc.facebiometricservice.config.BiometricProperties;
import com.isc.facebiometricservice.domain.FaceEmbedding;
import com.isc.facebiometricservice.domain.SimilarityResult;
import com.isc.facebiometricservice.model.FaceModelRegistry;
import com.isc.facebiometricservice.repository.FaceEmbeddingRepository;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class FaceMatchingService {
    private static final Logger log = LoggerFactory.getLogger(FaceMatchingService.class);
    public static final String CLIENT_MODEL_ID = FaceModelRegistry.CLIENT_MODEL_ID;
    public static final String CLIENT_MODEL_VERSION = FaceModelRegistry.CLIENT_MODEL_VERSION;

    private final FaceEmbeddingRepository repository;
    private final BiometricProperties properties;
    private final FaceMatcher matcher;
    private final FaceModelRegistry modelRegistry;

    public FaceMatchingService(FaceEmbeddingRepository repository,
                                BiometricProperties properties,
                                FaceMatcher matcher,
                                FaceModelRegistry modelRegistry) {
        this.repository = repository;
        this.properties = properties;
        this.matcher = matcher;
        this.modelRegistry = modelRegistry;
    }

    public void enroll(String userId, FaceEmbedding embedding) {
        log.info("[SERVER_EMBEDDING] enroll start user={} model={} version={} dimension={} normalized={}",
                userId, embedding == null ? null : embedding.modelId(),
                embedding == null ? null : embedding.modelVersion(),
                embedding == null ? null : embedding.dimension(),
                embedding != null && embedding.normalized());
        validate(embedding);
        repository.save(userId, embedding);
        log.info("[SERVER_EMBEDDING] enroll accepted user={} model={} version={}",
                userId, embedding.modelId(), embedding.modelVersion());
    }

    public SimilarityResult verify(String userId, FaceEmbedding probe) {
        long started = System.nanoTime();
        log.info("[SERVER_EMBEDDING] verify start user={} model={} version={} dimension={} normalized={}",
                userId, probe == null ? null : probe.modelId(),
                probe == null ? null : probe.modelVersion(),
                probe == null ? null : probe.dimension(),
                probe != null && probe.normalized());
        validate(probe);
        FaceEmbedding ref = repository.find(userId, probe.modelId(), probe.modelVersion())
                .orElseThrow(() -> new IllegalArgumentException("Reference embedding not found"));
        log.info("[SERVER_EMBEDDING] reference loaded user={} model={} version={} dimension={}",
                userId, ref.modelId(), ref.modelVersion(), ref.dimension());
        double similarity = matcher.similarity(probe.values(), ref.values());
        double threshold = properties.threshold();
        SimilarityResult result = new SimilarityResult(similarity, threshold, similarity >= threshold,
                matcher.algorithm(), (System.nanoTime() - started) / 1_000_000);
        log.info("[SERVER_EMBEDDING] verify decision user={} similarity={} threshold={} matched={} algorithm={} processingMs={}",
                userId, result.similarity(), result.threshold(), result.matched(),
                result.algorithm(), result.processingTimeMs());
        return result;
    }

    private void validate(FaceEmbedding e) {
        log.debug("[SERVER_EMBEDDING] validate model={} version={} dimension={} values={} normalized={}",
                e == null ? null : e.modelId(), e == null ? null : e.modelVersion(),
                e == null ? null : e.dimension(),
                e == null || e.values() == null ? null : e.values().length,
                e != null && e.normalized());
        if (e == null || e.values() == null || e.dimension() <= 0) {
            throw new IllegalArgumentException("Embedding is required");
        }

        var descriptor = modelRegistry.find(e.modelId(), e.modelVersion())
                .orElseThrow(() -> new IllegalArgumentException("Model/version mismatch"));

        if (e.dimension() != descriptor.dimension() || e.values().length != descriptor.dimension()) {
            throw new IllegalArgumentException("Embedding dimension mismatch");
        }
        if (descriptor.normalized() && !e.normalized()) {
            throw new IllegalArgumentException("Normalized embedding is required");
        }
        for (float value : e.values()) {
            if (!Float.isFinite(value)) {
                throw new IllegalArgumentException("Embedding contains non-finite values");
            }
        }
        if (descriptor.normalized() && !isL2Normalized(e.values())) {
            throw new IllegalArgumentException("Embedding is not L2-normalized");
        }
    }

    public boolean supportsModel(String modelId, String modelVersion) {
        return modelRegistry.supports(modelId, modelVersion);
    }

    private boolean isL2Normalized(float[] values) {
        double sum = 0.0;
        for (float value : values) {
            sum += (double) value * value;
        }
        double norm = Math.sqrt(sum);
        return Double.isFinite(norm) && Math.abs(norm - 1.0) <= 0.02;
    }
}
