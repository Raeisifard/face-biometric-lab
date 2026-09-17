package com.isc.facebiometricservice.streaming;

import com.isc.facebiometricservice.biometric.ReferenceEmbeddingRepository;
import com.isc.facebiometricservice.config.BiometricProperties;
import com.isc.facebiometricservice.videoverification.VideoVerificationEngine;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Autowired;

@Service
public class LiveStreamSessionManager {
    private static final int MINIMUM_RECOGNITION_FRAMES = 5;
    private final Duration sessionTtl;
    private final Map<String, VerificationSession> sessions = new ConcurrentHashMap<>();
    private final Map<String, FrameFeedback> latestFeedbackBySession = new ConcurrentHashMap<>();
    private final Map<String, Integer> frameCountsBySession = new ConcurrentHashMap<>();
    private final Map<String, Double> latestLivenessBySession = new ConcurrentHashMap<>();
    private final Map<String, List<Double>> livenessScoresBySession = new ConcurrentHashMap<>();
    private final Map<String, Boolean> livenessFailureBySession = new ConcurrentHashMap<>();
    private final Map<String, Double> temporalMotionBySession = new ConcurrentHashMap<>();
    private final Map<String, Boolean> temporalReadyBySession = new ConcurrentHashMap<>();
    private final Map<String, Boolean> temporalAvailableBySession = new ConcurrentHashMap<>();
    private final LiveFrameAnalyzer analyzer;
    private final ReferenceEmbeddingRepository references;
    private final BiometricProperties biometricProperties;
    private final VideoVerificationEngine recognitionEngine;
    private final Map<String, List<Double>> recognitionScoresBySession = new ConcurrentHashMap<>();

    public LiveStreamSessionManager() {
        this(Duration.ofMinutes(2), frame -> new LiveFrameAnalyzer.Analysis("GOOD_FRAME", "Frame accepted"));
    }

    public LiveStreamSessionManager(Duration sessionTtl) {
        this(sessionTtl, frame -> new LiveFrameAnalyzer.Analysis("GOOD_FRAME", "Frame accepted"));
    }

    public LiveStreamSessionManager(LiveFrameAnalyzer analyzer) {
        this(Duration.ofMinutes(2), analyzer);
    }

    public LiveStreamSessionManager(LiveFrameAnalyzer analyzer,
                                    ReferenceEmbeddingRepository references,
                                    BiometricProperties biometricProperties) {
        this(Duration.ofMinutes(2), analyzer, references, biometricProperties);
    }

    @Autowired
    public LiveStreamSessionManager(LiveFrameAnalyzer analyzer,
                                    ReferenceEmbeddingRepository references,
                                    BiometricProperties biometricProperties,
                                    VideoVerificationEngine recognitionEngine) {
        this(Duration.ofMinutes(2), analyzer, references, biometricProperties, recognitionEngine);
    }

    private LiveStreamSessionManager(Duration sessionTtl, LiveFrameAnalyzer analyzer) {
        this(sessionTtl, analyzer, null, null);
    }

    private LiveStreamSessionManager(Duration sessionTtl, LiveFrameAnalyzer analyzer,
                                     ReferenceEmbeddingRepository references,
                                     BiometricProperties biometricProperties) {
        this(sessionTtl, analyzer, references, biometricProperties, null);
    }

    private LiveStreamSessionManager(Duration sessionTtl, LiveFrameAnalyzer analyzer,
                                     ReferenceEmbeddingRepository references,
                                     BiometricProperties biometricProperties,
                                     VideoVerificationEngine recognitionEngine) {
        if (sessionTtl == null || sessionTtl.isNegative() || sessionTtl.isZero()) {
            throw new IllegalArgumentException("sessionTtl must be positive");
        }
        this.sessionTtl = sessionTtl;
        this.analyzer = analyzer;
        this.references = references;
        this.biometricProperties = biometricProperties;
        this.recognitionEngine = recognitionEngine;
    }

    public VerificationSession createSession(String customerReferenceId, String expectedCaptureMode) {
        String sessionId = UUID.randomUUID().toString();
        Instant now = Instant.now();
        VerificationSession session = new VerificationSession(
                sessionId,
                customerReferenceId,
                now,
                now.plus(sessionTtl),
                expectedCaptureMode,
                "CAPTURING",
                null, null, null, null, 0, 0, 0, List.of()
        );
        sessions.put(sessionId, session);
        latestFeedbackBySession.put(sessionId, new FrameFeedback("CAPTURE_CONTINUE", "Session started", "CAPTURING"));
        return session;
    }

    public VerificationSession getSession(String sessionId) {
        VerificationSession session = sessions.get(sessionId);
        if (session == null) {
            throw new IllegalArgumentException("Unknown session: " + sessionId);
        }
        return session;
    }

    public boolean isActive(String sessionId) {
        VerificationSession session = sessions.get(sessionId);
        return session != null && !isExpired(session);
    }

    public FrameUploadResult recordFrame(String sessionId, byte[] frame) {
        validateFrame(sessionId, frame);
        VerificationSession session = getSession(sessionId);
        int count = frameCountsBySession.merge(sessionId, 1, Integer::sum);
        int progress = Math.min(100, count * 20);
        LiveFrameAnalyzer.Analysis analysis = analyzer.analyze(sessionId, frame);
        if (recognitionEngine != null) {
            VideoVerificationEngine.LiveRecognition recognition = recognitionEngine.recognizeLiveFrame(frame, session.customerReferenceId());
            if (recognition.similarity() != null) {
                recognitionScoresBySession.computeIfAbsent(sessionId, ignored -> new CopyOnWriteArrayList<>()).add(recognition.similarity());
            }
        }
        if (analysis.livenessScore() != null) {
            latestLivenessBySession.put(sessionId, analysis.livenessScore());
            livenessScoresBySession.computeIfAbsent(sessionId, ignored -> new CopyOnWriteArrayList<>()).add(analysis.livenessScore());
        }
        if ("LIVENESS_FAILED".equals(analysis.feedbackCode())) {
            livenessFailureBySession.put(sessionId, true);
        }
        if (analysis.temporalMotion() != null) {
            temporalAvailableBySession.put(sessionId, true);
            temporalMotionBySession.put(sessionId, analysis.temporalMotion());
        }
        if (analysis.temporalReady()) temporalReadyBySession.put(sessionId, true);
        String feedbackCode = !"GOOD_FRAME".equals(analysis.feedbackCode()) ? analysis.feedbackCode() : count == 1 ? "GOOD_FRAME" : count < 5 ? "LIVENESS_PROGRESS" : "CAPTURE_CONTINUE";
        String state = count < 5 ? "LIVENESS_ANALYSIS" : "CAPTURING";
        String message = !"GOOD_FRAME".equals(analysis.feedbackCode()) ? analysis.message() : count < 5 ? "Frame accepted; collect more temporal evidence" : "Frame accepted; continue capture";
        FrameFeedback feedback = new FrameFeedback(feedbackCode, message, state);
        latestFeedbackBySession.put(sessionId, feedback);
        return new FrameUploadResult(sessionId, feedback.code(), feedback.state(), feedback.message(), frame.length, Instant.now(), count, progress,
            analysis.detectedFaces(), analysis.livenessScore());
    }

    public void validateFrame(String sessionId, byte[] frame) {
        if (sessionId == null || sessionId.isBlank()) {
            throw new IllegalArgumentException("sessionId is required");
        }
        VerificationSession session = sessions.get(sessionId);
        if (session == null) {
            throw new IllegalArgumentException("Unknown session: " + sessionId);
        }
        if (isExpired(session)) {
            throw new IllegalStateException("Session is expired: " + sessionId);
        }
        if (frame == null || frame.length == 0) {
            throw new IllegalArgumentException("Frame payload is required");
        }
        if (!"LIVE_STREAM".equalsIgnoreCase(session.expectedCaptureMode())) {
            throw new IllegalStateException("Session capture mode does not accept live frames");
        }
        if (!"CAPTURING".equals(session.processingState())
            && !"LIVENESS_ANALYSIS".equals(session.processingState())
            && !"RECOGNITION_PENDING".equals(session.processingState())) {
            throw new IllegalStateException("Session is no longer accepting frames: " + session.processingState());
        }
    }

    public void expireSession(String sessionId) {
        VerificationSession session = sessions.get(sessionId);
        if (session == null) {
            return;
        }
        sessions.put(sessionId, new VerificationSession(
                session.sessionId(),
                session.customerReferenceId(),
                session.startTime(),
                Instant.now().minusSeconds(1),
                session.expectedCaptureMode(),
                "EXPIRED",
                "SESSION_EXPIRED", "INCONCLUSIVE", null, null, 0, frameCountsBySession.getOrDefault(sessionId, 0), 0,
                List.of("SESSION_EXPIRED")
        ));
        latestFeedbackBySession.put(sessionId, new FrameFeedback("VERIFICATION_FAILED", "Session expired", "EXPIRED"));
    }

    public void markCaptureComplete(String sessionId) {
        VerificationSession session = getSession(sessionId);
        int frameCount = frameCountsBySession.getOrDefault(sessionId, 0);
        FrameFeedback latest = getLatestFeedback(sessionId);
        String finalResult = frameCount == 0
                ? "VERIFICATION_FAILED: NO_FRAME"
            : references != null && !referenceExists(session.customerReferenceId())
                ? "VERIFICATION_INCONCLUSIVE: REFERENCE_NOT_FOUND"
            : recognitionEngine != null && recognitionScoresBySession.getOrDefault(sessionId, List.of()).isEmpty()
                ? "VERIFICATION_INCONCLUSIVE: RECOGNITION_FAILED"
            : recognitionEngine != null && averageRecognition(sessionId) < biometricProperties.threshold()
                ? "VERIFICATION_COMPLETE: NO_MATCH"
            : Boolean.TRUE.equals(temporalAvailableBySession.get(sessionId))
                && !Boolean.TRUE.equals(temporalReadyBySession.get(sessionId))
                ? "VERIFICATION_INCONCLUSIVE: TEMPORAL_EVIDENCE_PENDING"
            : Boolean.TRUE.equals(livenessFailureBySession.get(sessionId))
                ? "VERIFICATION_FAILED: LIVENESS_FAILED"
                : switch (latest.code()) {
                    case "NO_FACE", "MULTIPLE_FACES", "INVALID_FRAME", "LIVENESS_FAILED" -> "VERIFICATION_FAILED: " + latest.code();
                    default -> frameCount >= MINIMUM_RECOGNITION_FRAMES
                            ? "VERIFICATION_COMPLETE: RECOGNITION_SUCCESS"
                            : "VERIFICATION_INCONCLUSIVE: RECOGNITION_PENDING";
                };
        String processingState = finalResult.endsWith("RECOGNITION_PENDING") || finalResult.endsWith("TEMPORAL_EVIDENCE_PENDING")
            ? "RECOGNITION_PENDING" : "VERIFICATION_COMPLETE";
        String result = finalResult.startsWith("VERIFICATION_FAILED") || finalResult.endsWith("NO_MATCH") ? "NO_MATCH"
            : finalResult.endsWith("RECOGNITION_PENDING") || finalResult.endsWith("TEMPORAL_EVIDENCE_PENDING") || finalResult.endsWith("REFERENCE_NOT_FOUND") ? "INCONCLUSIVE" : "MATCH";
        List<String> reasonCodes = finalResult.endsWith("REFERENCE_NOT_FOUND")
            ? List.of("REFERENCE_NOT_FOUND")
            : finalResult.endsWith("RECOGNITION_PENDING") || finalResult.endsWith("TEMPORAL_EVIDENCE_PENDING")
            ? List.of(finalResult.endsWith("TEMPORAL_EVIDENCE_PENDING") ? "TEMPORAL_EVIDENCE_PENDING" : "RECOGNITION_PENDING")
            : finalResult.endsWith("NO_MATCH")
            ? List.of("SIMILARITY_BELOW_THRESHOLD")
            : finalResult.startsWith("VERIFICATION_FAILED")
            ? List.of(finalResult.substring(finalResult.indexOf(':') + 2)) : List.of();
        VerificationSession updated = new VerificationSession(
                session.sessionId(),
                session.customerReferenceId(),
                session.startTime(),
                session.expirationTime(),
                session.expectedCaptureMode(),
                processingState,
                finalResult, result, averageRecognitionOrNull(sessionId), averageLiveness(sessionId), 0, frameCount, frameCount, reasonCodes
        );
        sessions.put(sessionId, updated);
            latestFeedbackBySession.put(sessionId, new FrameFeedback(
                finalResult.startsWith("VERIFICATION_FAILED") ? "VERIFICATION_FAILED"
                    : finalResult.endsWith("RECOGNITION_PENDING") ? "RECOGNITION_PENDING" : "VERIFICATION_COMPLETE",
                finalResult, processingState));
    }

    public FrameFeedback getLatestFeedback(String sessionId) {
        return latestFeedbackBySession.getOrDefault(sessionId, new FrameFeedback("CAPTURE_CONTINUE", "No feedback yet", "CAPTURING"));
    }

    private boolean isExpired(VerificationSession session) {
        return session.expirationTime().isBefore(Instant.now()) || session.expirationTime().equals(Instant.now());
    }

    private Double averageLiveness(String sessionId) {
        List<Double> scores = livenessScoresBySession.get(sessionId);
        if (scores == null || scores.isEmpty()) return null;
        return scores.stream().mapToDouble(Double::doubleValue).average().orElse(0);
    }

    private boolean referenceExists(String referenceId) {
        return biometricProperties != null
                && references.find(referenceId, biometricProperties.modelId(), biometricProperties.modelVersion()).isPresent();
    }

    private double averageRecognition(String sessionId) {
        return recognitionScoresBySession.getOrDefault(sessionId, List.of()).stream()
                .mapToDouble(Double::doubleValue).average().orElse(0);
    }

    private Double averageRecognitionOrNull(String sessionId) {
        List<Double> scores = recognitionScoresBySession.get(sessionId);
        return scores == null || scores.isEmpty() ? null : averageRecognition(sessionId);
    }
}
