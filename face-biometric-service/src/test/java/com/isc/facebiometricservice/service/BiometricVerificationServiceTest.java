package com.isc.facebiometricservice.service;

import com.isc.facebiometricservice.api.VerificationRequest;
import com.isc.facebiometricservice.api.VerificationStatus;
import com.isc.facebiometricservice.biometric.FaceMatcher;
import com.isc.facebiometricservice.biometric.ReferenceEmbeddingRepository;
import com.isc.facebiometricservice.config.BiometricProperties;
import com.isc.facebiometricservice.domain.FaceEmbedding;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class BiometricVerificationServiceTest {
    private final BiometricProperties properties = new BiometricProperties(
            "property", "arcface-512", "w600k-r50", "models/test.onnx", 3, 0.80, "COSINE", true,
            new BiometricProperties.Detector(false, "", 0.9),
            new BiometricProperties.Liveness(false, "", 0.5),
            new BiometricProperties.Oracle("", "", ""), new BiometricProperties.Mongo("", "", ""),
            new BiometricProperties.Cors("*"), Map.of());

    @Test void mapsAboveThresholdToMatch() {
        ReferenceEmbeddingRepository repo = (id, model, version) -> Optional.of(
                new FaceEmbedding(new float[]{1, 0, 0}, 3, model, version, true));
        BiometricVerificationService service = new BiometricVerificationService(repo, new com.isc.facebiometricservice.biometric.CosineFaceMatcher(), properties);
        var request = request(new float[]{1, 0, 0}, true);
        assertEquals(VerificationStatus.MATCH, service.verify(request).result());
    }

    @Test void mapsBelowThresholdToNoMatch() {
        ReferenceEmbeddingRepository repo = (id, model, version) -> Optional.of(
                new FaceEmbedding(new float[]{1, 0, 0}, 3, model, version, true));
        BiometricVerificationService service = new BiometricVerificationService(repo, new com.isc.facebiometricservice.biometric.CosineFaceMatcher(), properties);
        assertEquals(VerificationStatus.NO_MATCH, service.verify(request(new float[]{0, 1, 0}, true)).result());
    }

    @Test void missingReferenceIsInconclusive() {
        ReferenceEmbeddingRepository repo = (id, model, version) -> Optional.empty();
        BiometricVerificationService service = new BiometricVerificationService(repo, new com.isc.facebiometricservice.biometric.CosineFaceMatcher(), properties);
        assertEquals(VerificationStatus.INCONCLUSIVE, service.verify(request(new float[]{1, 0, 0}, true)).result());
    }

    private VerificationRequest request(float[] values, boolean normalized) {
        var embedding = new ArrayList<Float>();
        for (float value : values) embedding.add(value);
        return new VerificationRequest("req-1", "customer-1",
                new VerificationRequest.CaptureData("EMBEDDING", embedding, normalized), null, null, null);
    }
}
