package com.isc.facebiometricservice.fullclip;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/** Aggregates per-frame verification scores for full-clip verification. */
public final class FullClipScoreAggregator {
    public double aggregate(List<Double> scores, String aggregation) {
        if (scores == null || scores.isEmpty()) return 0.0;
        List<Double> values = scores.stream().filter(value -> value != null && Double.isFinite(value)).toList();
        if (values.isEmpty()) return 0.0;
        String mode = aggregation == null ? "MEAN" : aggregation.trim().toUpperCase(Locale.ROOT);
        return switch (mode) {
            case "MEAN" -> values.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
            case "MEDIAN" -> median(values);
            case "MAX" -> values.stream().max(Comparator.naturalOrder()).orElse(0.0);
            default -> throw new IllegalArgumentException("Unsupported aggregation: " + aggregation);
        };
    }

    private double median(List<Double> values) {
        List<Double> sorted = values.stream().sorted().toList();
        int middle = sorted.size() / 2;
        return (sorted.size() & 1) == 1 ? sorted.get(middle) : (sorted.get(middle - 1) + sorted.get(middle)) / 2.0;
    }
}