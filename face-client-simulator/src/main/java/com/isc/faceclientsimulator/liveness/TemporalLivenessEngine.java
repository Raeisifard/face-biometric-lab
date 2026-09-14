package com.isc.faceclientsimulator.liveness;

import com.isc.faceclientsimulator.config.SimulatorProperties;
import com.isc.faceclientsimulator.domain.FaceDetectionResult;
import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
public class TemporalLivenessEngine {
    private record Sample(double liveScore, double yaw, double cx, double cy) {}
    private final SimulatorProperties properties;
    private final ConcurrentMap<String, Deque<Sample>> sessions = new ConcurrentHashMap<>();

    public TemporalLivenessEngine(SimulatorProperties properties) { this.properties = properties; }

    public synchronized Result accept(String sessionId, FaceDetectionResult detection, double liveScore) {
        Deque<Sample> samples = sessions.computeIfAbsent(sessionId, k -> new ArrayDeque<>());
        if (!detection.faceDetected()) {
            samples.clear();
            return new Result("NO_FACE", false, 0, 0, 0, properties.livenessMinFrames(), "Center your face in the frame.");
        }
        double yaw = detection.landmarks().yawProxy(detection.box());
        samples.addLast(new Sample(liveScore, yaw, detection.box().centerX(), detection.box().centerY()));
        while (samples.size() > properties.livenessMinFrames()) samples.removeFirst();

        double avg = samples.stream().mapToDouble(Sample::liveScore).average().orElse(0);
        double yawRange = range(samples, Sample::yaw);
        double cxRange = range(samples, Sample::cx);
        double normalizedMotion = Math.min(1.0, Math.max(yawRange / 0.10, cxRange / 45.0));
        boolean enough = samples.size() >= properties.livenessMinFrames();
        boolean live = enough && avg >= properties.matchingLivenessThreshold() && normalizedMotion >= properties.requiredTemporalMotion();

        String status;
        String instruction;
        if (live) {
            status = "LIVE";
            instruction = "Liveness passed. You can verify or enroll.";
        } else if (avg < properties.matchingLivenessThreshold()) {
            status = "SUSPECTED_SPOOF";
            instruction = "Keep the face visible and avoid screens/photos.";
        } else {
            status = enough ? "MOVE_HEAD" : "COLLECTING";
            instruction = "Slowly move your head left and right.";
        }
        return new Result(status, live, avg, normalizedMotion, samples.size(), properties.livenessMinFrames(), instruction);
    }

    public void reset(String sessionId) { sessions.remove(sessionId); }

    private double range(Deque<Sample> samples, java.util.function.ToDoubleFunction<Sample> extractor) {
        if (samples.isEmpty()) return 0;
        double min = Double.MAX_VALUE, max = -Double.MAX_VALUE;
        for (Sample s : samples) {
            double v = extractor.applyAsDouble(s);
            min = Math.min(min, v); max = Math.max(max, v);
        }
        return Math.abs(max - min);
    }

    public record Result(String status, boolean live, double avgLiveScore, double temporalMotion, int acceptedFrames, int requiredFrames, String instruction) {}
}
