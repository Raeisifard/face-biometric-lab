package com.isc.facebiometricservice.biometric;

public final class CosineFaceMatcher implements FaceMatcher {
    @Override
    public double similarity(float[] a, float[] b) {
        if (a == null || b == null || a.length != b.length || a.length == 0) {
            throw new IllegalArgumentException("Vector dimensions do not match");
        }
        double dot = 0.0, na = 0.0, nb = 0.0;
        for (int i = 0; i < a.length; i++) {
            dot += (double) a[i] * b[i];
            na += (double) a[i] * a[i];
            nb += (double) b[i] * b[i];
        }
        if (na < 1e-12 || nb < 1e-12) {
            throw new IllegalArgumentException("Zero embedding");
        }
        return dot / (Math.sqrt(na) * Math.sqrt(nb));
    }

    @Override
    public String algorithm() { return "COSINE"; }
}
