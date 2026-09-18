package com.isc.facebiometricservice.api;

import com.isc.facebiometricservice.biometric.FaceMatcher;
import com.isc.facebiometricservice.config.BiometricProperties;
import com.isc.facebiometricservice.config.HybridMultiFrameProperties;
import com.isc.facebiometricservice.policy.BiometricPolicy;
import com.isc.facebiometricservice.policy.BiometricPolicyService;
import com.isc.facebiometricservice.policy.BiometricPolicyViolationException;
import com.isc.facebiometricservice.videoverification.HybridMultiFrameVerificationService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

@RestController
@RequestMapping("/api/v1/biometric/hybrid-multi-frame-verification")
public class HybridMultiFrameVerificationController {
    private final HybridMultiFrameVerificationService service;
    private final HybridMultiFrameProperties properties;
    private final BiometricProperties biometric;
    private final FaceMatcher matcher;
    private final BiometricPolicyService policyService;
    private final Map<String, Session> sessions = new ConcurrentHashMap<>();

    public HybridMultiFrameVerificationController(HybridMultiFrameVerificationService service,
                                                   HybridMultiFrameProperties properties,
                                                   BiometricProperties biometric,
                                                   FaceMatcher matcher,
                                                   BiometricPolicyService policyService) {
        this.service = service;
        this.properties = properties;
        this.biometric = biometric;
        this.matcher = matcher;
        this.policyService = policyService;
    }

    @PostMapping("/sessions")
    public ResponseEntity<?> createSession(@RequestParam String referenceId,
                                            @RequestParam(required = false) Integer requestedFrames) {
        try {
            if (!properties.enabled()) return invalid(null, referenceId, "HYBRID_MULTI_FRAME_DISABLED", "Hybrid multi-frame verification is disabled.");
            if (referenceId == null || referenceId.isBlank()) return invalid(null, referenceId, "REFERENCE_REQUIRED", "Reference ID is required.");

            BiometricPolicy policy = policyService.currentPolicy();
            policyService.validateMethod(policy, "HYBRID_MULTI_FRAME");
            int count = policy.capture().requiredFrameCount() > 0 ? policy.capture().requiredFrameCount() : properties.defaultFrames();
            if (requestedFrames != null && requestedFrames != count) {
                return invalid(null, referenceId, "POLICY_FRAME_COUNT_OVERRIDE",
                        "The requested frame count does not match the server-issued policy.");
            }
            if (count < 1 || count > properties.maxFrames()) {
                return invalid(null, referenceId, "FRAME_COUNT_NOT_ALLOWED", "The server policy requires an unsupported frame count.");
            }

            String id = UUID.randomUUID().toString();
            Instant expiresAt = Instant.now().plusSeconds(policy.sessionTtlSeconds());
            sessions.put(id, new Session(id, referenceId, count, expiresAt, false, policy.policyId(), policy.version()));
            return ResponseEntity.ok(new SessionResponse(id, referenceId, count, properties.maxFrames(),
                    policy.liveness().mode(), "MEAN", expiresAt, policy.policyId(), policy.version(),
                    policy.recognition().modelId(), policy.recognition().modelVersion(), policy.recognition().threshold()));
        } catch (BiometricPolicyViolationException ex) {
            return invalid(null, referenceId, ex.code(), ex.getMessage());
        }
    }

    @PostMapping(value = "/verify", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<VerificationResponse> verify(@RequestParam String sessionId,
                                                        @RequestParam String referenceId,
                                                        @RequestParam(required = false) List<Integer> sequenceNumbers,
                                                        @RequestParam(required = false) List<Long> captureTimestamps,
                                                        @RequestPart("images") List<MultipartFile> images) {
        String requestId = UUID.randomUUID().toString();
        Session session = sessions.get(sessionId);
        if (session == null) return invalid(requestId, referenceId, "SESSION_NOT_FOUND", "The verification session was not found.");
        if (!session.expiresAt().isAfter(Instant.now())) { sessions.remove(sessionId); return invalid(requestId, referenceId, "SESSION_EXPIRED", "The verification session has expired."); }
        if (!session.referenceId().equals(referenceId)) return invalid(requestId, referenceId, "REFERENCE_ID_MISMATCH", "The reference ID does not match the verification session.");
        if (!consume(sessionId)) return invalid(requestId, referenceId, "SESSION_REPLAYED", "The verification session has already been consumed.");
        try {
            BiometricPolicy policy = policyService.currentPolicy();
            if (!policy.policyId().equals(session.policyId()) || policy.version() != session.policyVersion()) {
                policy = policyService.policyFor(policy.profile());
            }
            policyService.validateMethod(policy, "HYBRID_MULTI_FRAME");
            if (images == null || images.isEmpty()) return invalid(requestId, referenceId, "FRAMES_REQUIRED", "At least one frame is required.");
            policyService.validateFrameCount(policy, images.size());
            if (images.size() > properties.maxFrames()) return invalid(requestId, referenceId, "TOO_MANY_FRAMES", "The submitted frame count exceeds the server maximum.");
            if (!validSequence(sequenceNumbers, images.size()) || !validTimestamps(captureTimestamps, images.size())) return invalid(requestId, referenceId, "INVALID_SEQUENCE", "Frame sequence metadata is invalid or not strictly ordered.");

            List<byte[]> frames = new java.util.ArrayList<>(images.size());
            long uploaded = 0;
            for (MultipartFile image : images) {
                if (image == null || image.isEmpty()) return invalid(requestId, referenceId, "INVALID_IMAGE", "A non-empty image is required for every frame.");
                if (image.getSize() > properties.maxFrameBytes()) return invalid(requestId, referenceId, "IMAGE_TOO_LARGE", "A submitted frame exceeds the configured size limit.");
                if (image.getContentType() == null || !image.getContentType().startsWith("image/")) return invalid(requestId, referenceId, "INVALID_IMAGE_TYPE", "Every submitted frame must be an image.");
                uploaded += image.getSize();
                frames.add(image.getBytes());
            }
            policyService.validatePayload(policy, uploaded);

            var outcome = service.verify(requestId, referenceId, frames);
            VerificationStatus status = VerificationStatus.valueOf(outcome.result());
            String code = primaryCode(outcome.result(), outcome.reasonCodes());
            VerificationResponse response = new VerificationResponse(
                    requestId, referenceId, "HYBRID_MULTI_FRAME", status, code, message(status, code),
                    status == VerificationStatus.INVALID_REQUEST ? 400 : status == VerificationStatus.PROCESSING_ERROR ? 500 : 200,
                    outcome.similarity(), biometric.threshold(),
                    new VerificationResponse.ModelMetadata(biometric.modelId(), biometric.modelVersion(), biometric.dimension(), matcher.algorithm()),
                    new VerificationResponse.QualityLiveness(outcome.qualityScore(), status == VerificationStatus.MATCH || status == VerificationStatus.NO_MATCH, outcome.livenessScore()),
                    new VerificationResponse.Metrics(outcome.processingMs(), outcome.submittedFrames(), outcome.recognitionFrames(), uploaded),
                    outcome.reasonCodes().isEmpty() ? List.of(code) : outcome.reasonCodes());
            return ResponseEntity.status(response.httpStatus()).body(response);
        } catch (BiometricPolicyViolationException ex) {
            return invalid(requestId, referenceId, ex.code(), ex.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new VerificationResponse(requestId, referenceId, "HYBRID_MULTI_FRAME",
                    VerificationStatus.PROCESSING_ERROR, "MODEL_ERROR", "Biometric verification could not be completed.", 500,
                    null, biometric.threshold(), new VerificationResponse.ModelMetadata(biometric.modelId(), biometric.modelVersion(), biometric.dimension(), matcher.algorithm()),
                    new VerificationResponse.QualityLiveness(null, null, null), new VerificationResponse.Metrics(0L, images == null ? 0 : images.size(), 0, null), List.of("MODEL_ERROR")));
        }
    }

    private boolean validSequence(List<Integer> sequence, int count) {
        if (sequence == null || sequence.size() != count) return false;
        for (int i = 0; i < count; i++) if (sequence.get(i) == null || sequence.get(i) != i) return false;
        return true;
    }

    private boolean validTimestamps(List<Long> timestamps, int count) {
        if (timestamps == null || timestamps.size() != count) return false;
        long previous = Long.MIN_VALUE;
        for (Long timestamp : timestamps) { if (timestamp == null || timestamp <= previous) return false; previous = timestamp; }
        return true;
    }

    private boolean consume(String id) {
        AtomicBoolean accepted = new AtomicBoolean(false);
        sessions.computeIfPresent(id, (key, value) -> {
            if (value.consumed()) return value;
            accepted.set(true);
            return new Session(value.sessionId(), value.referenceId(), value.expectedFrames(), value.expiresAt(), true, value.policyId(), value.policyVersion());
        });
        return accepted.get();
    }

    private ResponseEntity<VerificationResponse> invalid(String requestId, String referenceId, String code, String message) {
        VerificationResponse response = new VerificationResponse(requestId == null ? UUID.randomUUID().toString() : requestId,
                referenceId, "HYBRID_MULTI_FRAME", VerificationStatus.INVALID_REQUEST, code, message, 400, null, biometric.threshold(),
                new VerificationResponse.ModelMetadata(biometric.modelId(), biometric.modelVersion(), biometric.dimension(), matcher.algorithm()),
                new VerificationResponse.QualityLiveness(null, null, null), new VerificationResponse.Metrics(0L, null, null, null), List.of(code));
        return ResponseEntity.badRequest().body(response);
    }

    private String primaryCode(String result, List<String> reasons) {
        if (reasons != null && !reasons.isEmpty()) return reasons.get(reasons.size() - 1);
        return switch (result) { case "MATCH" -> "MATCH"; case "NO_MATCH" -> "SIMILARITY_BELOW_THRESHOLD"; default -> "VERIFICATION_INCONCLUSIVE"; };
    }

    private String message(VerificationStatus status, String code) {
        return switch (code) {
            case "MATCH" -> "Biometric verification matched the enrolled reference.";
            case "SIMILARITY_BELOW_THRESHOLD" -> "Biometric verification completed, but similarity is below the configured threshold.";
            case "LIVENESS_FAILED" -> "Server-side sequence liveness did not meet the configured threshold.";
            case "REFERENCE_NOT_FOUND" -> "No compatible biometric reference embedding is enrolled for this reference.";
            case "NO_VALID_FRAMES" -> "No submitted frame passed server-side biometric validation.";
            case "POLICY_FRAME_COUNT_OVERRIDE" -> "The client cannot override the server-issued frame count.";
            default -> status == VerificationStatus.INVALID_REQUEST ? "The biometric verification request is invalid." : "Biometric verification could not be completed.";
        };
    }

    public record SessionResponse(String sessionId, String referenceId, int expectedFrames, int maxFrames,
                                  String livenessMode, String aggregation, Instant expiresAt,
                                  String policyId, long policyVersion, String recognitionModelId,
                                  String recognitionModelVersion, double recognitionThreshold) {}
    private record Session(String sessionId, String referenceId, int expectedFrames, Instant expiresAt,
                           boolean consumed, String policyId, long policyVersion) {}
}
