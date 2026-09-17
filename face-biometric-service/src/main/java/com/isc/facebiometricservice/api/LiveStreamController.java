package com.isc.facebiometricservice.api;

import com.isc.facebiometricservice.biometric.FaceMatcher;
import com.isc.facebiometricservice.config.BiometricProperties;
import com.isc.facebiometricservice.streaming.FrameFeedback;
import com.isc.facebiometricservice.streaming.FrameUploadResult;
import com.isc.facebiometricservice.streaming.LiveStreamSessionManager;
import com.isc.facebiometricservice.streaming.VerificationSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.beans.factory.annotation.Value;

import java.util.List;

@RestController
@RequestMapping("/api/v1/live-stream")
public class LiveStreamController {
    private static final Logger log = LoggerFactory.getLogger(LiveStreamController.class);
    private final LiveStreamSessionManager sessionManager;
    private final BiometricProperties biometricProperties;
    private final FaceMatcher matcher;
    private final String configuredCaptureMethod;

    public LiveStreamController(LiveStreamSessionManager sessionManager,
                                BiometricProperties biometricProperties,
                                FaceMatcher matcher,
                                @Value("${biometric.capture-method:LIVE_STREAM}") String configuredCaptureMethod) {
        this.sessionManager = sessionManager;
        this.biometricProperties = biometricProperties;
        this.matcher = matcher;
        this.configuredCaptureMethod = configuredCaptureMethod.toUpperCase();
    }

    @PostMapping("/sessions")
    public SessionResponse createSession(@RequestParam(required = false) String customerReferenceId,
                                        @RequestParam(defaultValue = "LIVE_STREAM") String expectedCaptureMode) {
        String selectedMethod = expectedCaptureMode.toUpperCase();
        if (!"LIVE_STREAM".equals(selectedMethod)
                || !("LIVE_STREAM".equals(configuredCaptureMethod) || "FREE_METHOD".equals(configuredCaptureMethod))) {
            throw new IllegalArgumentException("CAPTURE_MODE_NOT_ALLOWED");
        }
        String referenceId = customerReferenceId == null || customerReferenceId.isBlank() ? "anonymous" : customerReferenceId;
        VerificationSession session = sessionManager.createSession(referenceId, selectedMethod);
        log.info("[LIVE][SESSION_CREATED] sessionId={} referenceId={} captureMode={}", session.sessionId(), referenceId, expectedCaptureMode);
        return new SessionResponse(session.sessionId(), session.customerReferenceId(), session.expectedCaptureMode(), session.processingState(), session.expirationTime());
    }

    @GetMapping("/sessions/{sessionId}")
    public VerificationSession getSession(@PathVariable String sessionId) {
        return sessionManager.getSession(sessionId);
    }

    @PostMapping(value = "/sessions/{sessionId}/frames", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public FrameUploadResult uploadFrame(@PathVariable String sessionId,
                                        @RequestPart("frame") MultipartFile frame) throws Exception {
        byte[] payload = frame == null ? null : frame.getBytes();
        FrameUploadResult result = sessionManager.recordFrame(sessionId, payload);
        log.debug("[LIVE][FRAME_ACCEPTED] sessionId={} feedback={} sizeBytes={}", sessionId, result.feedbackCode(), result.frameSizeBytes());
        return result;
    }

    @PostMapping("/sessions/{sessionId}/complete")
    public VerificationResponse complete(@PathVariable String sessionId) {
        sessionManager.markCaptureComplete(sessionId);
        VerificationSession session = sessionManager.getSession(sessionId);
        VerificationStatus status = VerificationStatus.valueOf(session.result());
        String code = primaryCode(session);
        String message = message(status, code);
        int httpStatus = status == VerificationStatus.INVALID_REQUEST ? 400 : status == VerificationStatus.PROCESSING_ERROR ? 500 : 200;
        return new VerificationResponse(
                session.sessionId(), session.customerReferenceId(), "LIVE_STREAM", status, code, message, httpStatus,
                session.similarity(), biometricProperties.threshold(),
                new VerificationResponse.ModelMetadata(biometricProperties.modelId(), biometricProperties.modelVersion(), biometricProperties.dimension(), matcher.algorithm()),
                new VerificationResponse.QualityLiveness(null, null, session.livenessScore()),
                new VerificationResponse.Metrics(session.processingTimeMs(), session.decodedFrames(), session.recognitionFrames(), null),
                session.reasonCodes() == null || session.reasonCodes().isEmpty() ? List.of(code) : session.reasonCodes());
    }

    @GetMapping("/sessions/{sessionId}/feedback")
    public FrameFeedback feedback(@PathVariable String sessionId) {
        return sessionManager.getLatestFeedback(sessionId);
    }

    @PostMapping("/sessions/{sessionId}/expire")
    public VerificationSession expire(@PathVariable String sessionId) {
        sessionManager.expireSession(sessionId);
        return sessionManager.getSession(sessionId);
    }

    private String primaryCode(VerificationSession session) {
        if (session.reasonCodes() != null && !session.reasonCodes().isEmpty()) return session.reasonCodes().get(0);
        return switch (session.result()) {
            case "MATCH" -> "MATCH";
            case "NO_MATCH" -> "SIMILARITY_BELOW_THRESHOLD";
            case "INCONCLUSIVE" -> "VERIFICATION_INCONCLUSIVE";
            default -> "PROCESSING_ERROR";
        };
    }

    private String message(VerificationStatus status, String code) {
        return switch (code) {
            case "MATCH" -> "Biometric verification matched the enrolled reference.";
            case "SIMILARITY_BELOW_THRESHOLD" -> "Biometric verification completed, but similarity is below the configured threshold.";
            case "REFERENCE_NOT_FOUND" -> "No compatible biometric reference embedding is enrolled for this reference.";
            case "LIVENESS_FAILED" -> "Liveness verification did not meet the configured threshold.";
            case "NO_FACE" -> "No face was detected in the submitted capture.";
            case "MULTIPLE_FACES" -> "Multiple faces were detected in the submitted capture.";
            case "TEMPORAL_EVIDENCE_PENDING" -> "Additional temporal liveness evidence is required before verification can complete.";
            case "RECOGNITION_PENDING" -> "Additional recognition frames are required before verification can complete.";
            default -> status == VerificationStatus.INVALID_REQUEST ? "The biometric verification request is invalid." : "Biometric verification could not be completed.";
        };
    }

    public record SessionResponse(String sessionId, String customerReferenceId, String expectedCaptureMode,
                                 String processingState, java.time.Instant expirationTime) {}
}
