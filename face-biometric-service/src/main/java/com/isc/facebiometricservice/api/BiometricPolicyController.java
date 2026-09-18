package com.isc.facebiometricservice.api;

import com.isc.facebiometricservice.policy.BiometricPolicy;
import com.isc.facebiometricservice.policy.BiometricPolicyService;
import com.isc.facebiometricservice.policy.BiometricPolicyViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/biometric/policy")
public class BiometricPolicyController {
    private final BiometricPolicyService service;
    public BiometricPolicyController(BiometricPolicyService service) { this.service = service; }

    @GetMapping
    public PolicyResponse current() { return response(service.currentPolicy(), null, null); }

    @GetMapping("/profiles")
    public Map<String, BiometricPolicy> profiles() { return service.profiles(); }

    @PostMapping("/sessions")
    public ResponseEntity<?> createSession(@RequestParam String referenceId,
                                           @RequestParam(required = false) String profile,
                                           @RequestParam(required = false) String requestedMethod) {
        try {
            var session = service.createSession(referenceId, profile);
            if (requestedMethod != null && !requestedMethod.isBlank()) service.validateMethod(session.policy(), requestedMethod);
            return ResponseEntity.ok(response(session.policy(), session.sessionId(), session.expiresAt()));
        } catch (BiometricPolicyViolationException ex) { return ResponseEntity.badRequest().body(error(ex.code(), ex.getMessage())); }
    }

    @GetMapping("/sessions/{sessionId}")
    public ResponseEntity<?> session(@PathVariable String sessionId) {
        try {
            var session = service.requireSession(sessionId);
            return ResponseEntity.ok(response(session.policy(), session.sessionId(), session.expiresAt()));
        } catch (BiometricPolicyViolationException ex) { return ResponseEntity.badRequest().body(error(ex.code(), ex.getMessage())); }
    }

    private PolicyResponse response(BiometricPolicy p, String sessionId, Instant expiresAt) {
        return new PolicyResponse(p.policyId(), p.version(), p.profile(), p.method().wireValue(),
                p.capture().minDurationSeconds(), p.capture().maxDurationSeconds(), p.capture().requiredFrameCount(),
                p.capture().uploadFps(), p.capture().maxPayloadBytes(), p.liveness().mode(), p.liveness().required(),
                p.liveness().threshold(), p.recognition().modelId(), p.recognition().modelVersion(), p.recognition().threshold(),
                p.fallbackMethod(), p.sessionTtlSeconds(), sessionId, expiresAt, true);
    }

    private ErrorResponse error(String code, String message) { return new ErrorResponse(UUID.randomUUID().toString(), code, message, List.of(code)); }

    public record PolicyResponse(String policyId, long version, String profile, String method,
                                 double minDurationSeconds, double maxDurationSeconds, int requiredFrameCount,
                                 double uploadFps, long maxPayloadBytes, String livenessMode, boolean livenessRequired,
                                 double livenessThreshold, String recognitionModelId, String recognitionModelVersion,
                                 double recognitionThreshold, String fallbackMethod, long sessionTtlSeconds,
                                 String sessionId, Instant expiresAt, boolean serverIssued) {}
    public record ErrorResponse(String requestId, String code, String message, List<String> reasonCodes) {}
}
