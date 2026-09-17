package com.isc.facebiometricservice.domain;

import java.util.Arrays;

public record FaceEmbedding(float[] values,int dimension,String modelId,String modelVersion,boolean normalized) {
    public FaceEmbedding {
        if(values==null||values.length!=512)throw new IllegalArgumentException("Embedding must contain exactly 512 values");
        if(dimension!=512)throw new IllegalArgumentException("Dimension must be 512");
        for (float value : values) {
            if (!Float.isFinite(value)) throw new IllegalArgumentException("Embedding values must be finite");
        }
        values=Arrays.copyOf(values,values.length);
    }
    @Override public float[] values(){return Arrays.copyOf(values,values.length);}
}
