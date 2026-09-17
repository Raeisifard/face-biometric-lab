package com.isc.facebiometricservice.api;

import com.isc.facebiometricservice.videoverification.VideoVerificationEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api/v1/biometric/hybrid-verification")
public class HybridBestFrameVerificationController {
    private static final Logger log = LoggerFactory.getLogger(HybridBestFrameVerificationController.class);
    private static final Duration SESSION_TTL = Duration.ofMinutes(2);
    private static final long MAX_IMAGE_BYTES = 8L * 1024 * 1024;

    private final VideoVerificationEngine engine;
    private final Map<String, Session> sessions = new ConcurrentHashMap<>();

    public HybridBestFrameVerificationController(VideoVerificationEngine engine) {
        this.engine = engine;
    }

    @PostMapping("/sessions")
    public SessionResponse createSession(@RequestParam String referenceId) {
        if (referenceId == null || referenceId.isBlank()) {
            throw new IllegalArgumentException("referenceId is required");
        }
        String sessionId = UUID.randomUUID().toString();
        Instant expiresAt = Instant.now().plus(SESSION_TTL);
        sessions.put(sessionId, new Session(sessionId, referenceId, expiresAt, false));
        log.info("Hybrid verification session created: sessionId={}, referenceId={}, expiresAt={}", sessionId, referenceId, expiresAt);
        return new SessionResponse(sessionId, referenceId, expiresAt);
    }

    @PostMapping(value = "/verify", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public HybridVerificationResponse verify(
            @RequestParam String sessionId,
            @RequestParam String referenceId,
            @RequestPart("image") MultipartFile image) {
        String requestId = UUID.randomUUID().toString();
        Session session = sessions.get(sessionId);
        if (session == null) {
            return invalid(requestId, referenceId, "SESSION_NOT_FOUND");
        }
        if (session.expiresAt().isBefore(Instant.now())) {
            sessions.remove(sessionId);
            return invalid(requestId, referenceId, "SESSION_EXPIRED");
        }
        if (!session.referenceId().equals(referenceId)) {
            return invalid(requestId, referenceId, "REFERENCE_ID_MISMATCH");
        }
        if (image == null || image.isEmpty()) {
            return invalid(requestId, referenceId, "INVALID_IMAGE");
        }
        if (image.getSize() > MAX_IMAGE_BYTES) {
            return invalid(requestId, referenceId, "IMAGE_TOO_LARGE");
        }
        String contentType = image.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            return invalid(requestId, referenceId, "INVALID_IMAGE_TYPE");
        }

        // The session is single-use. Mark it consumed before inference so a replay
        // cannot submit the same selected frame twice, even if processing fails.
        if (!consume(sessionId, session)) {
            return invalid(requestId, referenceId, "SESSION_REPLAYED");
        }

        try {
            VideoVerificationEngine.SingleImageOutcome outcome =
                    engine.verifySingleImage(requestId, image.getBytes(), referenceId);
            return new HybridVerificationResponse(
                    requestId,
                    sessionId,
                    referenceId,
                    Status.valueOf(outcome.result()),
                    outcome.similarity(),
                    outcome.livenessScore(),
                    outcome.qualityScore(),
                    outcome.processingMs(),
                    outcome.reasonCodes());
        } catch (IllegalArgumentException e) {
            log.warn("Hybrid verification rejected: requestId={}, sessionId={}, referenceId={}, reason={}",
                    requestId, sessionId, referenceId, e.getMessage());
            return invalid(requestId, referenceId, reason(e.getMessage()));
        } catch (Exception e) {
            log.error("Hybrid verification processing error: requestId={}, sessionId={}, referenceId={}",
                    requestId, sessionId, referenceId, e);
            return new HybridVerificationResponse(requestId, sessionId, referenceId,
                    Status.PROCESSING_ERROR, null, null, null, 0, List.of("MODEL_ERROR"));
        }
    }

    private boolean consume(String sessionId, Session expected) {
        return sessions.computeIfPresent(sessionId, (id, current) ->
                current.consumed() ? current : new Session(current.sessionId(), current.referenceId(), current.expiresAt(), true)) != null
                && sessions.get(sessionId).consumed();
    }

    private HybridVerificationResponse invalid(String requestId, String referenceId, String reason) {
        return new HybridVerificationResponse(requestId, null, referenceId, Status.INVALID_REQUEST,
                null, null, null, 0, List.of(reason));
    }

    private String reason(String value) {
        return switch (value == null ? "" : value) {
            case "INVALID_IMAGE", "SUSPICIOUS_IMAGE_DIMENSIONS", "IMAGE_TOO_LARGE",
                 "NO_FACE", "MULTIPLE_FACES", "FACE_TOO_SMALL", "LOW_QUALITY",
                 "LIVENESS_FAILED", "REFERENCE_NOT_FOUND", "MODEL_ERROR",
                 "SIMILARITY_BELOW_THRESHOLD" -> value;
            default -> "INVALID_IMAGE";
        };
    }

    public enum Status { MATCH, NO_MATCH, INCONCLUSIVE, INVALID_REQUEST, PROCESSING_ERROR }

    public record SessionResponse(String sessionId, String referenceId, Instant expiresAt) {}

    public record HybridVerificationResponse(
            String requestId,
            String sessionId,
            String referenceId,
            Status result,
            Double similarity,
            Double livenessScore,
            Double qualityScore,
            long processingTimeMs,
            List<String> reasonCodes) {}

    private record Session(String sessionId, String referenceId, Instant expiresAt, boolean consumed) {}
}
