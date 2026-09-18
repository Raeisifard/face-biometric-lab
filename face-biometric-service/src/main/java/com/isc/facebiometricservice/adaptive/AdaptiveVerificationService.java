package com.isc.facebiometricservice.adaptive;

import com.isc.facebiometricservice.api.EmbeddingPayload;
import com.isc.facebiometricservice.api.VerificationStatus;
import com.isc.facebiometricservice.biometric.FaceMatcher;
import com.isc.facebiometricservice.config.BiometricProperties;
import com.isc.facebiometricservice.domain.FaceEmbedding;
import com.isc.facebiometricservice.policy.BiometricPolicy;
import com.isc.facebiometricservice.policy.BiometricPolicyMethod;
import com.isc.facebiometricservice.policy.BiometricPolicyService;
import com.isc.facebiometricservice.policy.BiometricPolicyViolationException;
import com.isc.facebiometricservice.service.FaceMatchingService;
import com.isc.facebiometricservice.videoverification.HybridMultiFrameVerificationService;
import com.isc.facebiometricservice.videoverification.VideoClipDecoder;
import com.isc.facebiometricservice.videoverification.VideoVerificationEngine;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AdaptiveVerificationService {
    private static final Map<BiometricPolicyMethod,Integer> STRENGTH = Map.of(
            BiometricPolicyMethod.CLIENT_EMBEDDING, 1,
            BiometricPolicyMethod.HYBRID_SINGLE_FRAME, 2,
            BiometricPolicyMethod.HYBRID_MULTI_FRAME, 3,
            BiometricPolicyMethod.SERVER_LIVE_STREAM, 4,
            BiometricPolicyMethod.SERVER_FULL_CLIP, 5);
    private static final Set<String> ESCALATABLE = Set.of(
            "LOW_QUALITY", "INSUFFICIENT_LIVENESS_EVIDENCE", "LIVENESS_FAILED",
            "LOW_RECOGNITION_CONFIDENCE", "CAPTURE_INTERRUPTED", "INSUFFICIENT_FRAMES",
            "NO_FACE", "MULTIPLE_FACES", "FACE_TOO_SMALL", "NO_VALID_FRAMES");

    private final BiometricPolicyService policyService;
    private final VideoVerificationEngine engine;
    private final HybridMultiFrameVerificationService multiFrame;
    private final VideoClipDecoder decoder;
    private final FaceMatchingService faceMatching;
    private final BiometricProperties biometric;
    private final FaceMatcher matcher;
    private final Map<String, Session> sessions = new ConcurrentHashMap<>();

    public AdaptiveVerificationService(BiometricPolicyService policyService,
                                       VideoVerificationEngine engine,
                                       HybridMultiFrameVerificationService multiFrame,
                                       VideoClipDecoder decoder,
                                       FaceMatchingService faceMatching,
                                       BiometricProperties biometric,
                                       FaceMatcher matcher) {
        this.policyService = policyService;
        this.engine = engine;
        this.multiFrame = multiFrame;
        this.decoder = decoder;
        this.faceMatching = faceMatching;
        this.biometric = biometric;
        this.matcher = matcher;
    }

    public SessionView create(String referenceId, String profile, String requestedMethod) {
        var policySession = policyService.createSession(referenceId, profile, requestedMethod);
        BiometricPolicy policy = policySession.policy();
        String id = UUID.randomUUID().toString();
        Session session = new Session(id, referenceId, policySession.expiresAt(), policy,
                AdaptiveVerificationState.POLICY_ASSIGNED);
        session.history.add(event(session, "POLICY_ASSIGNED", null, null));
        sessions.put(id, session);
        return view(session);
    }

    public SessionView get(String sessionId) {
        Session s = require(sessionId);
        expireIfNeeded(s);
        return view(s);
    }

    public AttemptView verifyImage(String sessionId, byte[] image) {
        Session s = begin(sessionId);
        BiometricPolicyMethod method = s.policy.method();
        if (method != BiometricPolicyMethod.HYBRID_SINGLE_FRAME && method != BiometricPolicyMethod.SERVER_LIVE_STREAM) {
            return reject(s, "METHOD_INPUT_MISMATCH", "Image input is not valid for the current adaptive method");
        }
        if (image == null || image.length == 0) return reject(s, "INVALID_IMAGE", "A non-empty image is required");
        policyService.validatePayload(s.policy, image.length);
        VideoVerificationEngine.SingleImageOutcome outcome = method == BiometricPolicyMethod.HYBRID_SINGLE_FRAME
                ? engine.verifySingleImage(UUID.randomUUID().toString(), image, s.referenceId)
                : liveImage(image, s.referenceId);
        return evaluate(s, outcome.result(), first(outcome.reasonCodes()), outcome.reasonCodes(), outcome.similarity(),
                outcome.qualityScore(), outcome.livenessScore(), outcome.processingMs(), 1, 1);
    }

    private VideoVerificationEngine.SingleImageOutcome liveImage(byte[] image, String referenceId) {
        long started = System.nanoTime();
        var result = engine.recognizeLiveFrame(image, referenceId);
        if (!result.faceFound()) return new VideoVerificationEngine.SingleImageOutcome("INCONCLUSIVE", null, null, 0,
                elapsed(started), List.of(result.status().equals("NO_FACE") ? "NO_FACE" : result.status()));
        if (!result.referenceFound()) return new VideoVerificationEngine.SingleImageOutcome("INCONCLUSIVE", null, null, 0,
                elapsed(started), List.of("REFERENCE_NOT_FOUND"));
        String status = result.similarity() >= biometric.threshold() ? "MATCH" : "NO_MATCH";
        return new VideoVerificationEngine.SingleImageOutcome(status, result.similarity(), null, 1,
                elapsed(started), status.equals("MATCH") ? List.of() : List.of("SIMILARITY_BELOW_THRESHOLD"));
    }

    public AttemptView verifyFrames(String sessionId, List<byte[]> frames) {
        Session s = begin(sessionId);
        if (s.policy.method() != BiometricPolicyMethod.HYBRID_MULTI_FRAME) return reject(s, "METHOD_INPUT_MISMATCH", "Frame input is not valid for the current adaptive method");
        if (frames == null || frames.isEmpty()) return reject(s, "FRAMES_REQUIRED", "At least one frame is required");
        policyService.validateFrameCount(s.policy, frames.size());
        long payloadBytes = frames.stream().filter(Objects::nonNull).mapToLong(a -> a.length).sum();
        policyService.validatePayload(s.policy, payloadBytes);
        var outcome = multiFrame.verify(UUID.randomUUID().toString(), s.referenceId, frames);
        return evaluate(s, outcome.result(), first(outcome.reasonCodes()), outcome.reasonCodes(), outcome.similarity(),
                outcome.qualityScore(), outcome.livenessScore(), outcome.processingMs(), outcome.submittedFrames(), outcome.recognitionFrames());
    }

    public AttemptView verifyClip(String sessionId, MultipartFile clip) throws Exception {
        Session s = begin(sessionId);
        if (s.policy.method() != BiometricPolicyMethod.SERVER_FULL_CLIP) return reject(s, "METHOD_INPUT_MISMATCH", "Clip input is not valid for the current adaptive method");
        byte[] bytes = clip == null ? null : clip.getBytes();
        if (bytes == null || bytes.length == 0) return reject(s, "INVALID_VIDEO", "A non-empty video clip is required");
        policyService.validatePayload(s.policy, bytes.length);
        var decoded = decoder.decode(clip, 4.0, s.referenceId);
        policyService.validateDuration(s.policy, decoded.durationSeconds());
        var outcome = engine.verify(UUID.randomUUID().toString(), decoded, s.referenceId);
        return evaluate(s, outcome.result(), first(outcome.reasons()), outcome.reasons(), outcome.similarity(), null,
                outcome.livenessScore(), outcome.processingMs(), outcome.decodedFrames(), outcome.recognitionFrames());
    }

    public AttemptView verifyEmbedding(String sessionId, EmbeddingPayload payload) {
        Session s = begin(sessionId);
        if (s.policy.method() != BiometricPolicyMethod.CLIENT_EMBEDDING) return reject(s, "METHOD_INPUT_MISMATCH", "Embedding input is not valid for the current adaptive method");
        if (payload == null || !s.referenceId.equals(payload.userId())) return reject(s, "REFERENCE_ID_MISMATCH", "The reference ID does not match the adaptive session");
        try {
            var result = faceMatching.verify(payload.userId(), new FaceEmbedding(payload.embedding(), payload.dimension(), payload.modelId(), payload.modelVersion(), payload.normalized()));
            String status = result.matched() ? "MATCH" : "NO_MATCH";
            return evaluate(s, status, status.equals("MATCH") ? null : "SIMILARITY_BELOW_THRESHOLD",
                    status.equals("MATCH") ? List.of() : List.of("SIMILARITY_BELOW_THRESHOLD"), result.similarity(), null, null,
                    result.processingTimeMs(), 1, 1);
        } catch (IllegalArgumentException ex) {
            return evaluate(s, "INCONCLUSIVE", "MODEL_MISMATCH", List.of("MODEL_MISMATCH"), null, null, null, 0, 1, 0);
        }
    }

    private AttemptView evaluate(Session s, String result, String primaryReason, List<String> reasons, Double similarity,
                                 Double quality, Double liveness, long processingMs, int frames, int recognitionFrames) {
        transition(s, AdaptiveVerificationState.PROCESSING);
        transition(s, AdaptiveVerificationState.EVALUATED);
        String reason = primaryReason == null ? (reasons == null || reasons.isEmpty() ? null : reasons.get(0)) : primaryReason;
        if ("MATCH".equals(result) || "NO_MATCH".equals(result)) {
            transition(s, AdaptiveVerificationState.COMPLETED);
            s.finalResult = result;
            s.finalReason = reason;
            s.similarity = similarity;
            s.processingMs += processingMs;
            s.frames += frames;
            s.recognitionFrames += recognitionFrames;
            s.history.add(event(s, "COMPLETED", s.policy.method().wireValue(), reason));
            return attempt(s, result, reason, false);
        }
        if (!"INCONCLUSIVE".equals(result)) {
            transition(s, AdaptiveVerificationState.FAILED);
            s.finalResult = result;
            s.finalReason = reason;
            return attempt(s, result, reason, false);
        }
        if (shouldEscalate(s, reason)) {
            String next = s.policy.fallbackMethod();
            BiometricPolicy nextPolicy = policyService.escalationPolicyForMethod(next);
            transition(s, AdaptiveVerificationState.ESCALATING);
            s.history.add(event(s, "ESCALATING", nextPolicy.method().wireValue(), reason));
            s.policy = nextPolicy;
            s.attempts++;
            transition(s, AdaptiveVerificationState.CAPTURING);
            s.history.add(event(s, "POLICY_ASSIGNED", nextPolicy.method().wireValue(), "POLICY_REQUIRED"));
            s.processingMs += processingMs;
            s.frames += frames;
            s.recognitionFrames += recognitionFrames;
            return attempt(s, "INCONCLUSIVE", reason, true);
        }
        transition(s, AdaptiveVerificationState.COMPLETED);
        s.finalResult = "INCONCLUSIVE";
        s.finalReason = reason;
        s.processingMs += processingMs;
        s.frames += frames;
        s.recognitionFrames += recognitionFrames;
        s.history.add(event(s, "COMPLETED", s.policy.method().wireValue(), reason));
        return attempt(s, "INCONCLUSIVE", reason, false);
    }

    private boolean shouldEscalate(Session s, String reason) {
        String fallback = s.policy.fallbackMethod();
        if (fallback == null || fallback.isBlank() || !ESCALATABLE.contains(reason)) return false;
        BiometricPolicyMethod current = s.policy.method();
        BiometricPolicyMethod next = BiometricPolicyMethod.parse(fallback);
        return STRENGTH.getOrDefault(next, 0) > STRENGTH.getOrDefault(current, 0)
                && s.attempts < 4;
    }

    private Session begin(String id) {
        Session s = require(id);
        expireIfNeeded(s);
        if (s.state == AdaptiveVerificationState.COMPLETED || s.state == AdaptiveVerificationState.FAILED || s.state == AdaptiveVerificationState.EXPIRED)
            throw new AdaptiveVerificationException("SESSION_TERMINAL", "The adaptive verification session is already terminal");
        transition(s, AdaptiveVerificationState.CAPTURING);
        transition(s, AdaptiveVerificationState.PROCESSING);
        return s;
    }

    private AttemptView reject(Session s, String code, String message) {
        transition(s, AdaptiveVerificationState.FAILED);
        s.finalResult = "INVALID_REQUEST";
        s.finalReason = code;
        s.history.add(event(s, "FAILED", s.policy.method().wireValue(), code));
        return attempt(s, "INVALID_REQUEST", code, false);
    }

    private void expireIfNeeded(Session s) {
        if (!s.expiresAt.isAfter(Instant.now()) && s.state != AdaptiveVerificationState.COMPLETED) {
            s.state = AdaptiveVerificationState.EXPIRED;
            s.finalResult = "INCONCLUSIVE";
            s.finalReason = "SESSION_EXPIRED";
            s.history.add(event(s, "EXPIRED", s.policy.method().wireValue(), "SESSION_EXPIRED"));
            throw new AdaptiveVerificationException("SESSION_EXPIRED", "The adaptive verification session has expired");
        }
    }

    private Session require(String id) {
        Session s = sessions.get(id);
        if (s == null) throw new AdaptiveVerificationException("SESSION_NOT_FOUND", "The adaptive verification session was not found");
        return s;
    }

    private void transition(Session s, AdaptiveVerificationState next) {
        s.state = next;
    }

    private Event event(Session s, String transition, String method, String reason) {
        return new Event(Instant.now(), transition, method, reason, s.policy.policyId(), s.policy.version());
    }

    private AttemptView attempt(Session s, String result, String reason, boolean escalated) {
        return new AttemptView(s.sessionId, s.state, s.policy.method().wireValue(), result, reason, escalated,
                s.similarity, s.processingMs, s.frames, s.recognitionFrames, List.copyOf(s.history));
    }

    private SessionView view(Session s) {
        return new SessionView(s.sessionId, s.referenceId, s.state, s.policy.policyId(), s.policy.version(),
                s.policy.method().wireValue(), s.policy.fallbackMethod(), s.expiresAt, s.attempts,
                s.finalResult, s.finalReason, s.similarity, s.processingMs, s.frames, s.recognitionFrames, List.copyOf(s.history));
    }

    private String first(List<String> values) { return values == null || values.isEmpty() ? null : values.get(0); }
    private long elapsed(long started) { return (System.nanoTime() - started) / 1_000_000; }

    public record SessionView(String sessionId, String referenceId, AdaptiveVerificationState state,
                              String policyId, long policyVersion, String method, String fallbackMethod,
                              Instant expiresAt, int attempts, String finalResult, String finalReason,
                              Double similarity, long processingMs, int frameCount, int recognitionFrames,
                              List<Event> history) {}
    public record AttemptView(String sessionId, AdaptiveVerificationState state, String method, String result,
                              String reason, boolean escalated, Double similarity, long processingMs,
                              int frameCount, int recognitionFrames, List<Event> history) {}
    public record Event(Instant timestamp, String transition, String method, String reason, String policyId, long policyVersion) {}

    private static final class Session {
        final String sessionId;
        final String referenceId;
        final Instant expiresAt;
        BiometricPolicy policy;
        AdaptiveVerificationState state;
        int attempts = 1;
        String finalResult;
        String finalReason;
        Double similarity;
        long processingMs;
        int frames;
        int recognitionFrames;
        final List<Event> history = new ArrayList<>();
        Session(String sessionId, String referenceId, Instant expiresAt, BiometricPolicy policy, AdaptiveVerificationState state) {
            this.sessionId = sessionId; this.referenceId = referenceId; this.expiresAt = expiresAt; this.policy = policy; this.state = state;
        }
    }

    public static class AdaptiveVerificationException extends RuntimeException {
        private final String code;
        public AdaptiveVerificationException(String code, String message) { super(message); this.code = code; }
        public String code() { return code; }
    }
}
