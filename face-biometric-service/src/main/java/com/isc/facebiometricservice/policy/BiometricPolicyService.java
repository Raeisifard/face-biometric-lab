package com.isc.facebiometricservice.policy;

import com.isc.facebiometricservice.config.BiometricPolicyProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class BiometricPolicyService {
    private static final long DEFAULT_MAX_PAYLOAD = 25L * 1024 * 1024;
    private final BiometricPolicyProperties properties;
    private final Map<String, PolicySession> sessions = new ConcurrentHashMap<>();
    private final String legacyCaptureMethod;

    public BiometricPolicyService(BiometricPolicyProperties properties,
                                  @Value("${biometric.capture-method:FREE_METHOD}") String legacyCaptureMethod) {
        this.properties = properties;
        this.legacyCaptureMethod = legacyCaptureMethod;
    }

    public BiometricPolicy currentPolicy() { return policyFor(properties.defaultProfile()); }

    public BiometricPolicy policyFor(String requestedProfile) {
        String profile = requestedProfile == null || requestedProfile.isBlank()
                ? properties.defaultProfile() : requestedProfile.toUpperCase();
        var definition = properties.profiles().get(profile);
        if (definition == null) throw new BiometricPolicyViolationException("UNKNOWN_POLICY_PROFILE",
                "No biometric policy profile is configured for " + profile);
        String methodValue = definition.method();
        if (methodValue == null || methodValue.isBlank() || "FREE_METHOD".equalsIgnoreCase(methodValue)) methodValue = legacyCaptureMethod;
        if ("FREE_METHOD".equalsIgnoreCase(methodValue)) methodValue = "HYBRID_MULTI_FRAME";
        BiometricPolicyMethod method;
        try { method = BiometricPolicyMethod.parse(methodValue); }
        catch (IllegalArgumentException ex) { throw new BiometricPolicyViolationException("UNSUPPORTED_METHOD", "Unsupported policy method: " + methodValue); }
        long ttl = positive(definition.sessionTtlSeconds(), 120);
        long maxPayload = positive(definition.maxPayloadBytes(), DEFAULT_MAX_PAYLOAD);
        int frames = Math.max(0, definition.requiredFrameCount());
        double fps = Math.max(0, definition.uploadFps());
        double minDuration = Math.max(0, definition.minDurationSeconds());
        double maxDuration = Math.max(minDuration, definition.maxDurationSeconds());
        String livenessMode = text(definition.livenessMode(), "PASSIVE");
        String modelId = text(definition.recognitionModelId(), "arcface-512");
        String modelVersion = text(definition.recognitionModelVersion(), "w600k-r50");
        double threshold = definition.threshold() > 0 ? definition.threshold() : 0.65;
        String fallback = definition.fallbackMethod() == null || definition.fallbackMethod().isBlank() ? null : BiometricPolicyMethod.parse(definition.fallbackMethod()).wireValue();
        long version = positive(definition.version(), 1);
        return new BiometricPolicy("biometric-" + profile.toLowerCase(), version, profile, method,
                new BiometricPolicy.CaptureRequirements(minDuration, maxDuration, frames, fps, maxPayload),
                new BiometricPolicy.LivenessRequirements(livenessMode, !"NONE".equalsIgnoreCase(livenessMode), definition.threshold() > 0 ? 0.50 : 0.50),
                Math.max(0, definition.minQualityScore()),
                new BiometricPolicy.RecognitionRequirements(modelId, modelVersion, threshold), fallback, ttl, Instant.now());
    }

    public PolicySession createSession(String referenceId, String profile) {
        if (!properties.enabled()) throw new BiometricPolicyViolationException("POLICY_ENGINE_DISABLED", "Biometric policy engine is disabled");
        if (referenceId == null || referenceId.isBlank()) throw new BiometricPolicyViolationException("REFERENCE_REQUIRED", "Reference ID is required");
        BiometricPolicy policy = policyFor(profile);
        Instant expiresAt = Instant.now().plusSeconds(policy.sessionTtlSeconds());
        String id = UUID.randomUUID().toString();
        PolicySession session = new PolicySession(id, referenceId, policy, expiresAt);
        sessions.put(id, session);
        return session;
    }

    public PolicySession requireSession(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) throw new BiometricPolicyViolationException("POLICY_SESSION_REQUIRED", "Policy session ID is required");
        PolicySession session = sessions.get(sessionId);
        if (session == null) throw new BiometricPolicyViolationException("POLICY_SESSION_NOT_FOUND", "The biometric policy session was not found");
        if (!session.expiresAt().isAfter(Instant.now())) {
            sessions.remove(sessionId);
            throw new BiometricPolicyViolationException("SESSION_EXPIRED", "The biometric policy session has expired");
        }
        return session;
    }

    public void validateMethod(BiometricPolicy policy, String actualMethod) {
        BiometricPolicyMethod actual;
        try { actual = BiometricPolicyMethod.parse(actualMethod); }
        catch (IllegalArgumentException ex) { throw new BiometricPolicyViolationException("UNSUPPORTED_METHOD", "The requested biometric method is not supported"); }
        if (actual != policy.method()) throw new BiometricPolicyViolationException("POLICY_METHOD_MISMATCH",
                "The client method " + actual.wireValue() + " does not match the server-issued method " + policy.method().wireValue());
    }

    public void validateMethod(String sessionId, String actualMethod) { validateMethod(requireSession(sessionId).policy(), actualMethod); }

    public void validateFrameCount(BiometricPolicy policy, int count) {
        int required = policy.capture().requiredFrameCount();
        if (required > 0 && count != required) throw new BiometricPolicyViolationException("FRAME_COUNT_MISMATCH", "Expected " + required + " frames but received " + count);
    }

    public void validatePayload(BiometricPolicy policy, long bytes) {
        if (bytes < 0 || bytes > policy.capture().maxPayloadBytes()) throw new BiometricPolicyViolationException("PAYLOAD_TOO_LARGE", "Payload exceeds the server-issued maximum");
    }

    public void validateDuration(BiometricPolicy policy, double seconds) {
        if (seconds < policy.capture().minDurationSeconds() || seconds > policy.capture().maxDurationSeconds()) throw new BiometricPolicyViolationException("DURATION_VIOLATION", "Capture duration is outside the server-issued policy range");
    }

    public void validateLiveness(BiometricPolicy policy, Double score) {
        if (policy.liveness().required() && (score == null || score < policy.liveness().threshold())) throw new BiometricPolicyViolationException("LIVENESS_REQUIREMENT_VIOLATION", "Liveness evidence does not meet the server-issued requirement");
    }

    public void validateModel(BiometricPolicy policy, String modelId, String modelVersion) {
        if (!policy.recognition().modelId().equals(modelId) || !policy.recognition().modelVersion().equals(modelVersion)) throw new BiometricPolicyViolationException("MODEL_MISMATCH", "The biometric model does not match the server-issued policy");
    }

    public Map<String, BiometricPolicy> profiles() {
        Map<String, BiometricPolicy> result = new LinkedHashMap<>();
        properties.profiles().keySet().stream().sorted().forEach(name -> result.put(name, policyFor(name)));
        return result;
    }

    private long positive(long value, long fallback) { return value > 0 ? value : fallback; }
    private String text(String value, String fallback) { return value == null || value.isBlank() ? fallback : value; }

    public record PolicySession(String sessionId, String referenceId, BiometricPolicy policy, Instant expiresAt) {}
}
