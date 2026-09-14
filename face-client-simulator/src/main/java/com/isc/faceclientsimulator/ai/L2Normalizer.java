package com.isc.faceclientsimulator.ai;

import org.springframework.stereotype.Component;

@Component
public class L2Normalizer {
    public float[] normalize(float[] input) {
        double sum = 0;
        for (float x : input) sum += (double)x * x;
        double norm = Math.sqrt(sum);
        if (norm < 1e-12) throw new IllegalArgumentException("Zero embedding cannot be normalized");
        float[] out = new float[input.length];
        for (int i=0;i<input.length;i++) out[i] = (float)(input[i] / norm);
        return out;
    }
}
