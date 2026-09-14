package com.isc.faceclientsimulator.domain;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class FaceEmbeddingTest {
    @Test void only512IsAccepted(){
        assertThrows(IllegalArgumentException.class, () -> new FaceEmbedding(new float[511],511,"m","v",true));
        assertDoesNotThrow(() -> new FaceEmbedding(new float[512],512,"m","v",true));
    }
}
