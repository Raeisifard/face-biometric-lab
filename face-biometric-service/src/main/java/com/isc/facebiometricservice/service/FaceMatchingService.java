package com.isc.facebiometricservice.service;

import com.isc.facebiometricservice.config.BiometricProperties;
import com.isc.facebiometricservice.domain.FaceEmbedding;
import com.isc.facebiometricservice.domain.SimilarityResult;
import com.isc.facebiometricservice.repository.FaceEmbeddingRepository;
import org.springframework.stereotype.Service;

@Service
public class FaceMatchingService {
    private final FaceEmbeddingRepository repository;
    private final BiometricProperties properties;

    public FaceMatchingService(FaceEmbeddingRepository repository, BiometricProperties properties) {
        this.repository = repository;
        this.properties = properties;
    }

    public void enroll(String userId, FaceEmbedding embedding) {
        validate(embedding);
        repository.save(userId, embedding);
    }

    public SimilarityResult verify(String userId, FaceEmbedding probe) {
        long started = System.nanoTime();
        validate(probe);
        FaceEmbedding ref = repository.find(userId, probe.modelId(), probe.modelVersion()).orElseThrow(() -> new IllegalArgumentException("Reference embedding not found"));
        double similarity = cosine(probe.values(), ref.values());
        double threshold = properties.threshold();
        return new SimilarityResult(similarity, threshold, similarity >= threshold, properties.algorithm(), (System.nanoTime() - started) / 1_000_000);
    }

    private void validate(FaceEmbedding e) {
        if (e.dimension() != properties.dimension() || e.values().length != properties.dimension())
            throw new IllegalArgumentException("Embedding dimension mismatch");
        if (!properties.modelId().equals(e.modelId()) || !properties.modelVersion().equals(e.modelVersion()))
            throw new IllegalArgumentException("Model/version mismatch");
        if (properties.normalizedRequired() && !e.normalized())
            throw new IllegalArgumentException("Normalized embedding is required");
    }

    private double cosine(float[] a, float[] b) {
        if (a.length != b.length) throw new IllegalArgumentException("Vector dimensions do not match");
        double dot = 0, na = 0, nb = 0;
        for (int i = 0; i < a.length; i++) {
            dot += (double) a[i] * b[i];
            na += (double) a[i] * a[i];
            nb += (double) b[i] * b[i];
        }
        if (na < 1e-12 || nb < 1e-12) throw new IllegalArgumentException("Zero embedding");
        return dot / (Math.sqrt(na) * Math.sqrt(nb));
    }
}
