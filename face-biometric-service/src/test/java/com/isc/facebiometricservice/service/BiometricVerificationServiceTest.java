package com.isc.facebiometricservice.service;

import com.isc.facebiometricservice.api.VerificationRequest;
import com.isc.facebiometricservice.api.VerificationStatus;
import com.isc.facebiometricservice.biometric.CosineFaceMatcher;
import com.isc.facebiometricservice.biometric.FaceMatcher;
import com.isc.facebiometricservice.biometric.ReferenceEmbeddingRepository;
import com.isc.facebiometricservice.config.BiometricProperties;
import com.isc.facebiometricservice.domain.FaceEmbedding;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BiometricVerificationServiceTest {
    private static final int EMBEDDING_DIMENSION = 512;

    private final BiometricProperties properties = new BiometricProperties(
            "property", "arcface-512", "w600k-r50", "models/test.onnx", EMBEDDING_DIMENSION,
            0.80, "COSINE", true,
            new BiometricProperties.Detector(false, "", 0.9),
            new BiometricProperties.Liveness(false, "", 0.5),
            new BiometricProperties.Oracle("", "", ""), new BiometricProperties.Mongo("", "", ""),
            new BiometricProperties.Cors("*"), Map.of());

    @Test
    void mapsAboveThresholdToMatch() {
        float[] referenceValues = unitVector(0);
        ReferenceEmbeddingRepository repo = (id, model, version) -> Optional.of(
                new FaceEmbedding(referenceValues, EMBEDDING_DIMENSION, model, version, true));
        FaceMatcher matcher = new CosineFaceMatcher();
        BiometricVerificationService service = new BiometricVerificationService(repo, matcher, properties);

        assertEquals(VerificationStatus.MATCH, service.verify(request(referenceValues, true)).result());
    }

    @Test
    void mapsBelowThresholdToNoMatch() {
        float[] referenceValues = unitVector(0);
        float[] probeValues = unitVector(1);
        ReferenceEmbeddingRepository repo = (id, model, version) -> Optional.of(
                new FaceEmbedding(referenceValues, EMBEDDING_DIMENSION, model, version, true));
        FaceMatcher matcher = new CosineFaceMatcher();
        BiometricVerificationService service = new BiometricVerificationService(repo, matcher, properties);

        assertEquals(VerificationStatus.NO_MATCH, service.verify(request(probeValues, true)).result());
    }

    @Test
    void missingReferenceIsInconclusive() {
        ReferenceEmbeddingRepository repo = (id, model, version) -> Optional.empty();
        FaceMatcher matcher = new CosineFaceMatcher();
        BiometricVerificationService service = new BiometricVerificationService(repo, matcher, properties);

        assertEquals(VerificationStatus.INCONCLUSIVE, service.verify(request(unitVector(0), true)).result());
    }

    @Test
    void acceptsTheSeparateMobileFaceNetClientProfile() {
        FaceMatchingService service = new FaceMatchingService(null, properties, new CosineFaceMatcher());

        assertTrue(service.supportsModel(FaceMatchingService.CLIENT_MODEL_ID, FaceMatchingService.CLIENT_MODEL_VERSION));
    }

    @Test
    void rejectsNonFiniteEmbeddingValues() {
        float[] values = unitVector(0);
        values[1] = Float.NaN;
        assertThrows(IllegalArgumentException.class,
                () -> new FaceEmbedding(values, EMBEDDING_DIMENSION, "arcface-512", "w600k-r50", true));
    }

    private VerificationRequest request(float[] values, boolean normalized) {
        var embedding = new ArrayList<Float>(values.length);
        for (float value : values) {
            embedding.add(value);
        }
        return new VerificationRequest("req-1", "customer-1",
                new VerificationRequest.CaptureData("EMBEDDING", embedding, normalized), null, null, null);
    }

    private static float[] unitVector(int activeIndex) {
        float[] values = new float[EMBEDDING_DIMENSION];
        Arrays.fill(values, 0.0f);
        values[activeIndex] = 1.0f;
        return values;
    }
}
