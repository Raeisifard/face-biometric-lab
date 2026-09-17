package com.isc.facebiometricservice.videoverification;

import com.isc.facebiometricservice.config.BiometricProperties;
import com.isc.facebiometricservice.config.VideoVerificationProperties;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class HybridMultiFrameVerificationService {
    private final VideoVerificationEngine engine;
    private final BiometricProperties biometric;
    private final VideoVerificationProperties properties;

    public HybridMultiFrameVerificationService(VideoVerificationEngine engine,
                                               BiometricProperties biometric,
                                               VideoVerificationProperties properties) {
        this.engine = engine;
        this.biometric = biometric;
        this.properties = properties;
    }

    public Outcome verify(String requestId, String referenceId, List<byte[]> frames) {
        long start = System.nanoTime();
        if (frames == null || frames.isEmpty()) {
            return outcome("INVALID_REQUEST", null, 0, 0, elapsed(start), 0, 0, List.of("FRAMES_REQUIRED"));
        }

        List<Double> similarities = new ArrayList<>();
        List<Double> qualities = new ArrayList<>();
        List<Double> liveness = new ArrayList<>();
        List<String> reasons = new ArrayList<>();

        for (int i = 0; i < frames.size(); i++) {
            var result = engine.verifySingleImage(requestId + "-frame-" + (i + 1), frames.get(i), referenceId);
            if (result.similarity() != null && result.qualityScore() >= properties.minQualityScore()) {
                similarities.add(result.similarity());
                qualities.add(result.qualityScore());
                liveness.add(result.livenessScore() == null ? 0.0 : result.livenessScore());
            } else if (result.reasonCodes() != null && !result.reasonCodes().isEmpty()) {
                reasons.add("FRAME_" + (i + 1) + "_" + result.reasonCodes().get(0));
            }
        }

        if (similarities.isEmpty()) {
            if (reasons.isEmpty()) reasons.add("NO_VALID_FRAMES");
            return outcome("INCONCLUSIVE", null, average(liveness), average(qualities), elapsed(start), frames.size(), 0, reasons);
        }

        double averageLiveness = average(liveness);
        double minimumLiveness = liveness.stream().mapToDouble(Double::doubleValue).min().orElse(0);
        double quality = average(qualities);
        double similarity = aggregate(similarities, properties.aggregation());

        // Client-side liveness is never trusted. Every frame has been independently
        // decoded, detected, quality-checked, liveness-scored and recognized here.
        // Requiring both average and minimum liveness keeps one weak frame from being
        // hidden by several good frames.
        if (properties.livenessRequired()
                && (averageLiveness < properties.livenessThreshold()
                || minimumLiveness < properties.livenessThreshold())) {
            reasons.add("LIVENESS_FAILED");
            return outcome("INCONCLUSIVE", similarity, averageLiveness, quality, elapsed(start),
                    frames.size(), similarities.size(), reasons);
        }

        String result = similarity >= biometric.threshold() ? "MATCH" : "NO_MATCH";
        if ("NO_MATCH".equals(result)) reasons.add("SIMILARITY_BELOW_THRESHOLD");

        return outcome(result, similarity, averageLiveness, quality, elapsed(start),
                frames.size(), similarities.size(), reasons);
    }

    private Outcome outcome(String result, Double similarity, double liveness, double quality,
                            long processingMs, int submittedFrames, int recognitionFrames,
                            List<String> reasons) {
        return new Outcome(result, similarity, liveness, quality, processingMs,
                submittedFrames, recognitionFrames, reasons == null ? List.of() : List.copyOf(reasons));
    }

    private double average(List<Double> values) {
        return values.isEmpty() ? 0 : values.stream().mapToDouble(Double::doubleValue).average().orElse(0);
    }

    private double aggregate(List<Double> values, String configured) {
        if (values.isEmpty()) return 0;
        String mode = configured == null ? "MEAN" : configured.toUpperCase();
        return switch (mode) {
            case "MIN" -> values.stream().mapToDouble(Double::doubleValue).min().orElse(0);
            case "MEDIAN" -> {
                double[] sorted = values.stream().mapToDouble(Double::doubleValue).sorted().toArray();
                yield sorted[sorted.length / 2];
            }
            case "MAX" -> values.stream().mapToDouble(Double::doubleValue).max().orElse(0);
            default -> values.stream().mapToDouble(Double::doubleValue).average().orElse(0);
        };
    }

    private long elapsed(long start) {
        return (System.nanoTime() - start) / 1_000_000;
    }

    public record Outcome(String result, Double similarity, double livenessScore, double qualityScore,
                          long processingMs, int submittedFrames, int recognitionFrames,
                          List<String> reasonCodes) {}
}
