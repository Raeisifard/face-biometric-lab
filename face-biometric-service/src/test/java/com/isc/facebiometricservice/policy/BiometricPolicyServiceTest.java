package com.isc.facebiometricservice.policy;

import com.isc.facebiometricservice.config.BiometricPolicyProperties;
import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class BiometricPolicyServiceTest {

    @Test
    void selectsConfiguredMethodAndBindsSession() {
        var definition = new BiometricPolicyProperties.PolicyDefinition(
                "HYBRID_MULTI_FRAME",   // method
                2,                      // minDurationSeconds
                6,                      // maxDurationSeconds
                4,                      // requiredFrameCount
                4,                      // uploadFps
                10_000,                 // maxPayloadBytes
                "PASSIVE",              // livenessMode
                0.50,                   // livenessThreshold
                0.45,                   // minQualityScore
                "arcface-512",          // recognitionModelId
                "w600k-r50",            // recognitionModelVersion
                0.65,                   // threshold
                "HYBRID_SINGLE_FRAME",  // fallbackMethod
                120,                    // sessionTtlSeconds
                7);                     // version

        var service = new BiometricPolicyService(
                new BiometricPolicyProperties(
                        true,                   // enabled
                        "NORMAL",               // defaultProfile
                        "SERVER_ASSIGNED",      // selectionMode
                        Map.of("NORMAL", definition),  // profiles
                        Map.of()),              // methodPolicies
                "FREE_METHOD");

        var session = service.createSession("user-1", "NORMAL");
        assertEquals("HYBRID_MULTI_FRAME", session.policy().method().wireValue());
        assertEquals(7, session.policy().version());
        assertEquals(session.policy().version(),
                service.requireSession(session.sessionId()).policy().version());
        assertThrows(BiometricPolicyViolationException.class,
                () -> service.validateMethod(session.policy(), "FULL_CLIP"));
    }

    @Test
    void rejectsFrameCountPayloadDurationLivenessAndModelViolations() {
        var definition = new BiometricPolicyProperties.PolicyDefinition(
                "HYBRID_MULTI_FRAME",   // method
                2,                      // minDurationSeconds
                6,                      // maxDurationSeconds
                4,                      // requiredFrameCount
                4,                      // uploadFps
                1000,                   // maxPayloadBytes
                "PASSIVE",              // livenessMode
                0.45,                   // livenessThreshold
                0.45,                   // minQualityScore
                "arcface-512",          // recognitionModelId
                "w600k-r50",            // recognitionModelVersion
                0.65,                   // threshold
                null,                   // fallbackMethod
                120,                    // sessionTtlSeconds
                1);                     // version

        var service = new BiometricPolicyService(
                new BiometricPolicyProperties(
                        true,
                        "NORMAL",
                        "SERVER_ASSIGNED",
                        Map.of("NORMAL", definition),
                        Map.of()),
                "FREE_METHOD");

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
        var service = new BiometricPolicyService(
                new BiometricPolicyProperties(
                        true,
                        "NORMAL",
                        "SERVER_ASSIGNED",
                        Map.of(),       // profiles: empty -> HIGH_RISK must not resolve
                        Map.of()),      // methodPolicies: empty
                "FREE_METHOD");

        assertThrows(BiometricPolicyViolationException.class, () -> service.policyFor("HIGH_RISK"));
    }

    @Test
    void devModeResolvesPolicyForRequestedMethod() {
        var normal = new BiometricPolicyProperties.PolicyDefinition(
                "HYBRID_MULTI_FRAME",   // method
                1,                      // minDurationSeconds
                6,                      // maxDurationSeconds
                4,                      // requiredFrameCount
                4,                      // uploadFps
                10_000,                 // maxPayloadBytes
                "PASSIVE",              // livenessMode
                0.50,                   // livenessThreshold
                0.45,                   // minQualityScore
                "arcface-512",          // recognitionModelId
                "w600k-r50",            // recognitionModelVersion
                0.65,                   // threshold
                "HYBRID_SINGLE_FRAME",  // fallbackMethod
                120,                    // sessionTtlSeconds
                1);                     // version

        var live = new BiometricPolicyProperties.PolicyDefinition(
                "LIVE_STREAM",          // method
                2,                      // minDurationSeconds
                8,                      // maxDurationSeconds
                5,                      // requiredFrameCount
                4,                      // uploadFps
                8_000,                  // maxPayloadBytes
                "PASSIVE",              // livenessMode
                0.50,                   // livenessThreshold
                0.45,                   // minQualityScore
                "arcface-512",          // recognitionModelId
                "w600k-r50",            // recognitionModelVersion
                0.65,                   // threshold
                "HYBRID_SINGLE_FRAME",  // fallbackMethod
                120,                    // sessionTtlSeconds
                1);                     // version

        var service = new BiometricPolicyService(
                new BiometricPolicyProperties(
                        true,
                        "NORMAL",
                        "CLIENT_SELECTABLE",            // dev-only mode
                        Map.of("NORMAL", normal),       // profiles
                        Map.of("LIVE_STREAM", live)),   // methodPolicies
                "FREE_METHOD");

        var policy = service.policyForMethod("LIVE_STREAM");
        assertEquals(BiometricPolicyMethod.SERVER_LIVE_STREAM, policy.method());
        assertEquals("DEV", policy.profile());
        assertTrue(service.clientSelectable());
    }

    @Test
    void serverAssignedModeRejectsDifferentRequestedMethod() {
        var normal = new BiometricPolicyProperties.PolicyDefinition(
                "HYBRID_MULTI_FRAME",   // method
                1,                      // minDurationSeconds
                6,                      // maxDurationSeconds
                4,                      // requiredFrameCount
                4,                      // uploadFps
                10_000,                 // maxPayloadBytes
                "PASSIVE",              // livenessMode
                0.50,                   // livenessThreshold
                0.45,                   // minQualityScore
                "arcface-512",          // recognitionModelId
                "w600k-r50",            // recognitionModelVersion
                0.65,                   // threshold
                null,                   // fallbackMethod
                120,                    // sessionTtlSeconds
                1);                     // version

        var service = new BiometricPolicyService(
                new BiometricPolicyProperties(
                        true,
                        "NORMAL",
                        "SERVER_ASSIGNED",              // production-like mode
                        Map.of("NORMAL", normal),       // profiles
                        Map.of("LIVE_STREAM", normal)), // methodPolicies present, but client must not select it
                "FREE_METHOD");

        assertThrows(BiometricPolicyViolationException.class,
                () -> service.policyForMethod("LIVE_STREAM"));
    }
}