package com.isc.facebiometricservice.api;

import com.isc.facebiometricservice.streaming.FrameUploadResult;
import com.isc.facebiometricservice.streaming.FrameFeedback;
import com.isc.facebiometricservice.streaming.LiveStreamSessionManager;
import com.isc.facebiometricservice.streaming.VerificationSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/live-stream")
public class LiveStreamController {
    private static final Logger log = LoggerFactory.getLogger(LiveStreamController.class);
    private final LiveStreamSessionManager sessionManager;

    public LiveStreamController(LiveStreamSessionManager sessionManager) {
        this.sessionManager = sessionManager;
    }

    @PostMapping("/sessions")
    public SessionResponse createSession(@RequestParam(required = false) String customerReferenceId,
                                        @RequestParam(defaultValue = "LIVE_STREAM") String expectedCaptureMode) {
        String referenceId = customerReferenceId == null || customerReferenceId.isBlank() ? "anonymous" : customerReferenceId;
        VerificationSession session = sessionManager.createSession(referenceId, expectedCaptureMode);
        log.info("Live stream session created: sessionId={}, customerReferenceId={}, captureMode={}", session.sessionId(), referenceId, expectedCaptureMode);
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
        log.debug("Live stream frame accepted: sessionId={}, feedback={}, sizeBytes={}", sessionId, result.feedbackCode(), result.frameSizeBytes());
        return result;
    }

    @PostMapping("/sessions/{sessionId}/complete")
    public VerificationSession complete(@PathVariable String sessionId) {
        sessionManager.markCaptureComplete(sessionId);
        return sessionManager.getSession(sessionId);
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

    public record SessionResponse(String sessionId, String customerReferenceId, String expectedCaptureMode,
                                 String processingState, java.time.Instant expirationTime) {
    }
}
