package com.isc.facebiometricservice.biometric;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CosineFaceMatcherTest {
    private final FaceMatcher matcher = new CosineFaceMatcher();

    @Test void identicalNormalizedVectorsHaveSimilarityOne() {
        assertEquals(1.0, matcher.similarity(new float[]{1, 0, 0}, new float[]{1, 0, 0}), 1e-9);
    }

    @Test void orthogonalVectorsHaveSimilarityZero() {
        assertEquals(0.0, matcher.similarity(new float[]{1, 0}, new float[]{0, 1}), 1e-9);
    }

    @Test void zeroVectorIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> matcher.similarity(new float[]{0, 0}, new float[]{1, 0}));
    }
}
