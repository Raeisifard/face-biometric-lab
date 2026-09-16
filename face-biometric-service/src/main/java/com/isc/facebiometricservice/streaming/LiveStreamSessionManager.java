package com.isc.facebiometricservice.streaming;

import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Autowired;

@Service
public class LiveStreamSessionManager {
    private final Duration sessionTtl;
    private final Map<String, VerificationSession> sessions = new ConcurrentHashMap<>();
    private final Map<String, FrameFeedback> latestFeedbackBySession = new ConcurrentHashMap<>();
    private final Map<String, Integer> frameCountsBySession = new ConcurrentHashMap<>();
    private final LiveFrameAnalyzer analyzer;

    public LiveStreamSessionManager() {
        this(Duration.ofMinutes(2), frame -> new LiveFrameAnalyzer.Analysis("GOOD_FRAME", "Frame accepted"));
    }

    public LiveStreamSessionManager(Duration sessionTtl) {
        this(sessionTtl, frame -> new LiveFrameAnalyzer.Analysis("GOOD_FRAME", "Frame accepted"));
    }

    @Autowired
    public LiveStreamSessionManager(LiveFrameAnalyzer analyzer) {
        this(Duration.ofMinutes(2), analyzer);
    }

    private LiveStreamSessionManager(Duration sessionTtl, LiveFrameAnalyzer analyzer) {
        if (sessionTtl == null || sessionTtl.isNegative() || sessionTtl.isZero()) {
            throw new IllegalArgumentException("sessionTtl must be positive");
        }
        this.sessionTtl = sessionTtl;
        this.analyzer = analyzer;
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
                null
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
        LiveFrameAnalyzer.Analysis analysis = analyzer.analyze(frame);
        String feedbackCode = !"GOOD_FRAME".equals(analysis.feedbackCode()) ? analysis.feedbackCode() : count == 1 ? "GOOD_FRAME" : count < 5 ? "LIVENESS_PROGRESS" : "CAPTURE_CONTINUE";
        String state = count < 5 ? "LIVENESS_ANALYSIS" : "CAPTURING";
        String message = !"GOOD_FRAME".equals(analysis.feedbackCode()) ? analysis.message() : count < 5 ? "Frame accepted; collect more temporal evidence" : "Frame accepted; continue capture";
        FrameFeedback feedback = new FrameFeedback(feedbackCode, message, state);
        latestFeedbackBySession.put(sessionId, feedback);
        return new FrameUploadResult(sessionId, feedback.code(), feedback.state(), feedback.message(), frame.length, Instant.now(), count, progress);
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
        if (!"CAPTURING".equals(session.processingState()) && !"LIVENESS_ANALYSIS".equals(session.processingState())) {
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
                "SESSION_EXPIRED"
        ));
        latestFeedbackBySession.put(sessionId, new FrameFeedback("VERIFICATION_FAILED", "Session expired", "EXPIRED"));
    }

    public void markCaptureComplete(String sessionId) {
        VerificationSession session = getSession(sessionId);
        VerificationSession updated = new VerificationSession(
                session.sessionId(),
                session.customerReferenceId(),
                session.startTime(),
                session.expirationTime(),
                session.expectedCaptureMode(),
                "CAPTURE_COMPLETE",
                "CAPTURE_COMPLETE"
        );
        sessions.put(sessionId, updated);
        latestFeedbackBySession.put(sessionId, new FrameFeedback("CAPTURE_COMPLETE", "Capture completed", "CAPTURE_COMPLETE"));
    }

    public FrameFeedback getLatestFeedback(String sessionId) {
        return latestFeedbackBySession.getOrDefault(sessionId, new FrameFeedback("CAPTURE_CONTINUE", "No feedback yet", "CAPTURING"));
    }

    private boolean isExpired(VerificationSession session) {
        return session.expirationTime().isBefore(Instant.now()) || session.expirationTime().equals(Instant.now());
    }
}
