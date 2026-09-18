package com.isc.facebiometricservice.service;

import com.isc.facebiometricservice.api.VerificationRequest;
import com.isc.facebiometricservice.api.VerificationStatus;
import com.isc.facebiometricservice.biometric.CosineFaceMatcher;
import com.isc.facebiometricservice.biometric.FaceMatcher;
import com.isc.facebiometricservice.biometric.ReferenceEmbeddingRepository;
import com.isc.facebiometricservice.config.BiometricProperties;
import com.isc.facebiometricservice.domain.FaceEmbedding;
import com.isc.facebiometricservice.model.FaceModelRegistry;
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

    private final FaceModelRegistry registry = new FaceModelRegistry(properties);

    @Test
    void mapsAboveThresholdToMatch() {
        float[] referenceValues = unitVector(0);
        ReferenceEmbeddingRepository repo = (id, model, version) -> Optional.of(
                new FaceEmbedding(referenceValues, EMBEDDING_DIMENSION, model, version, true));
        FaceMatcher matcher = new CosineFaceMatcher();
        BiometricVerificationService service = service(repo, matcher);

        assertEquals(VerificationStatus.MATCH, service.verify(request(referenceValues, true, "arcface-512", "w600k-r50")).result());
    }

    @Test
    void mapsBelowThresholdToNoMatch() {
        float[] referenceValues = unitVector(0);
        float[] probeValues = unitVector(1);
        ReferenceEmbeddingRepository repo = (id, model, version) -> Optional.of(
                new FaceEmbedding(referenceValues, EMBEDDING_DIMENSION, model, version, true));
        FaceMatcher matcher = new CosineFaceMatcher();
        BiometricVerificationService service = service(repo, matcher);

        assertEquals(VerificationStatus.NO_MATCH, service.verify(request(probeValues, true, "arcface-512", "w600k-r50")).result());
    }

    @Test
    void missingReferenceIsInconclusive() {
        ReferenceEmbeddingRepository repo = (id, model, version) -> Optional.empty();
        FaceMatcher matcher = new CosineFaceMatcher();
        BiometricVerificationService service = service(repo, matcher);

        assertEquals(VerificationStatus.INCONCLUSIVE,
                service.verify(request(unitVector(0), true, "arcface-512", "w600k-r50")).result());
    }

    @Test
    void missingModelMetadataIsRejected() {
        ReferenceEmbeddingRepository repo = (id, model, version) -> Optional.empty();
        BiometricVerificationService service = service(repo, new CosineFaceMatcher());

        VerificationResponseAssertions.assertCode(
                service.verify(requestWithoutModel(unitVector(0), true)),
                "MODEL_METADATA_REQUIRED");
    }

    @Test
    void unsupportedModelIsConflict() {
        ReferenceEmbeddingRepository repo = (id, model, version) -> Optional.empty();
        BiometricVerificationService service = service(repo, new CosineFaceMatcher());

        var response = service.verify(request(unitVector(0), true, "unknown-model", "v1"));
        assertEquals(VerificationStatus.INVALID_REQUEST, response.result());
        assertEquals("MODEL_MISMATCH", response.code());
        assertEquals(409, response.httpStatus());
    }

    @Test
    void mismatchedReferenceModelIsConflict() {
        ReferenceEmbeddingRepository repo = new ReferenceEmbeddingRepository() {
            @Override
            public Optional<FaceEmbedding> find(String id, String model, String version) {
                return Optional.empty();
            }

            @Override
            public Optional<FaceEmbedding> findByReferenceId(String id) {
                return Optional.of(new FaceEmbedding(
                        unitVector(0), EMBEDDING_DIMENSION, "arcface-512", "w600k-r50", true));
            }
        };

        BiometricVerificationService service = service(repo, new CosineFaceMatcher());
        var response = service.verify(request(unitVector(0), true,
                FaceModelRegistry.CLIENT_MODEL_ID, FaceModelRegistry.CLIENT_MODEL_VERSION));

        assertEquals("MODEL_MISMATCH", response.code());
        assertEquals(409, response.httpStatus());
        assertEquals(FaceModelRegistry.CLIENT_MODEL_ID, response.model().modelId());
    }

    @Test
    void acceptsTheSeparateMobileFaceNetClientProfile() {
        assertTrue(registry.supports(FaceModelRegistry.CLIENT_MODEL_ID, FaceModelRegistry.CLIENT_MODEL_VERSION));
    }

    @Test
    void rejectsNonFiniteEmbeddingValues() {
        float[] values = unitVector(0);
        values[1] = Float.NaN;
        assertThrows(IllegalArgumentException.class,
                () -> new FaceEmbedding(values, EMBEDDING_DIMENSION, "arcface-512", "w600k-r50", true));
    }

    private BiometricVerificationService service(ReferenceEmbeddingRepository repo, FaceMatcher matcher) {
        return new BiometricVerificationService(repo, matcher, properties, registry);
    }

    private VerificationRequest request(float[] values, boolean normalized, String modelId, String modelVersion) {
        var embedding = new ArrayList<Float>(values.length);
        for (float value : values) {
            embedding.add(value);
        }
        return new VerificationRequest("req-1", "customer-1",
                new VerificationRequest.CaptureData("EMBEDDING", embedding, normalized),
                new VerificationRequest.ModelMetadata(modelId, modelVersion, EMBEDDING_DIMENSION),
                null, null);
    }

    private VerificationRequest requestWithoutModel(float[] values, boolean normalized) {
        var embedding = new ArrayList<Float>(values.length);
        for (float value : values) {
            embedding.add(value);
        }
        return new VerificationRequest("req-1", "customer-1",
                new VerificationRequest.CaptureData("EMBEDDING", embedding, normalized),
                null, null, null);
    }

    private static float[] unitVector(int activeIndex) {
        float[] values = new float[EMBEDDING_DIMENSION];
        Arrays.fill(values, 0.0f);
        values[activeIndex] = 1.0f;
        return values;
    }

    private static final class VerificationResponseAssertions {
        private static void assertCode(com.isc.facebiometricservice.api.VerificationResponse response, String code) {
            assertEquals(code, response.code());
        }
    }
}
