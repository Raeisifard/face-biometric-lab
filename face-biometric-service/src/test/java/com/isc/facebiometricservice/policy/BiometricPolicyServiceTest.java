package com.isc.facebiometricservice.policy;

import com.isc.facebiometricservice.config.BiometricPolicyProperties;
import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class BiometricPolicyServiceTest {
    @Test
    void selectsConfiguredMethodAndBindsSession() {
        var definition = new BiometricPolicyProperties.PolicyDefinition("HYBRID_MULTI_FRAME", 2, 6, 4, 4, 10_000, "PASSIVE", 0.50, 0.45, "arcface-512", "w600k-r50", 0.65, "HYBRID_SINGLE_FRAME", 120, 7);
        var service = new BiometricPolicyService(new BiometricPolicyProperties(true, "NORMAL", "SERVER_ASSIGNED", Map.of("NORMAL", definition), Map.of()), "FREE_METHOD");
        var session = service.createSession("user-1", "NORMAL");
        assertEquals("HYBRID_MULTI_FRAME", session.policy().method().wireValue());
        assertEquals(7, session.policy().version());
        assertEquals(session.policy().version(), service.requireSession(session.sessionId()).policy().version());
        assertThrows(BiometricPolicyViolationException.class, () -> service.validateMethod(session.policy(), "FULL_CLIP"));
    }

    @Test
    void rejectsFrameCountPayloadDurationLivenessAndModelViolations() {
        var definition = new BiometricPolicyProperties.PolicyDefinition("HYBRID_MULTI_FRAME", 2, 6, 4, 4, 1000, "PASSIVE", 0.45, "arcface-512", "w600k-r50", 0.65, null, 120, 1);
        var service = new BiometricPolicyService(new BiometricPolicyProperties(true, "NORMAL", Map.of("NORMAL", definition)), "FREE_METHOD");
        var policy = service.currentPolicy();
        assertThrows(BiometricPolicyViolationException.class, () -> service.validateFrameCount(policy, 1));
        assertThrows(BiometricPolicyViolationException.class, () -> service.validatePayload(policy, 1001));
        assertThrows(BiometricPolicyViolationException.class, () -> service.validateDuration(policy, 1));
        assertThrows(BiometricPolicyViolationException.class, () -> service.validateQuality(policy, 0.1));
        assertThrows(BiometricPolicyViolationException.class, () -> service.validateLiveness(policy, 0.1));
        assertThrows(BiometricPolicyViolationException.class, () -> service.validateModel(policy, "other", "version"));
    }

    @Test
    void rejectsUnknownProfile() {
        var service = new BiometricPolicyService(new BiometricPolicyProperties(true, "NORMAL", "SERVER_ASSIGNED", Map.of(), Map.of()), "FREE_METHOD");
        assertThrows(BiometricPolicyViolationException.class, () -> service.policyFor("HIGH_RISK"));
    }
}
