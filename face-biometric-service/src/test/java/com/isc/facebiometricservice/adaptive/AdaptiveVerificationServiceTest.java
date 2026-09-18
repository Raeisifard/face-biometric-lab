package com.isc.facebiometricservice.adaptive;

import com.isc.facebiometricservice.api.EmbeddingPayload;
import com.isc.facebiometricservice.policy.BiometricPolicy;
import com.isc.facebiometricservice.policy.BiometricPolicyMethod;
import com.isc.facebiometricservice.policy.BiometricPolicyService;
import com.isc.facebiometricservice.service.FaceMatchingService;
import com.isc.facebiometricservice.videoverification.HybridMultiFrameVerificationService;
import com.isc.facebiometricservice.videoverification.VideoClipDecoder;
import com.isc.facebiometricservice.videoverification.VideoVerificationEngine;
import com.isc.facebiometricservice.biometric.FaceMatcher;
import com.isc.facebiometricservice.config.BiometricProperties;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AdaptiveVerificationServiceTest {

    @Test
    void singleFrameInconclusiveEscalatesToMultiFrameThenMatches() {
        BiometricPolicyService policies = mock(BiometricPolicyService.class);
        VideoVerificationEngine engine = mock(VideoVerificationEngine.class);
        HybridMultiFrameVerificationService multi = mock(HybridMultiFrameVerificationService.class);
        BiometricPolicy single = policy(BiometricPolicyMethod.HYBRID_SINGLE_FRAME, "HYBRID_MULTI_FRAME");
        BiometricPolicy frames = policy(BiometricPolicyMethod.HYBRID_MULTI_FRAME, "SERVER_FULL_CLIP");
        when(policies.createSession(eq("user-1"), isNull(), eq("HYBRID_SINGLE_FRAME")))
                .thenReturn(new BiometricPolicyService.PolicySession("policy-1", "user-1", single, Instant.now().plusSeconds(120)));
        when(policies.escalationPolicyForMethod("HYBRID_MULTI_FRAME")).thenReturn(frames);
        when(engine.verifySingleImage(anyString(), any(), eq("user-1")))
                .thenReturn(new VideoVerificationEngine.SingleImageOutcome("INCONCLUSIVE", null, 0.1, 0.2, 5, List.of("LOW_QUALITY")));
        when(multi.verify(anyString(), eq("user-1"), anyList()))
                .thenReturn(new HybridMultiFrameVerificationService.Outcome("MATCH", 0.91, 0.9, 0.8, 12, 4, 4, List.of()));

        AdaptiveVerificationService service = new AdaptiveVerificationService(policies, engine, multi,
                mock(VideoClipDecoder.class), mock(FaceMatchingService.class), mock(BiometricProperties.class), mock(FaceMatcher.class));

        var created = service.create("user-1", null, "HYBRID_SINGLE_FRAME");
        assertEquals(AdaptiveVerificationState.POLICY_ASSIGNED, created.state());
        var escalated = service.verifyImage(created.sessionId(), new byte[]{1});
        assertTrue(escalated.escalated());
        assertEquals("HYBRID_MULTI_FRAME", escalated.method());
        assertEquals(AdaptiveVerificationState.CAPTURING, escalated.state());
        var completed = service.verifyFrames(created.sessionId(), List.of(new byte[]{1}, new byte[]{2}, new byte[]{3}, new byte[]{4}));
        assertEquals("MATCH", completed.result());
        assertFalse(completed.escalated());
        assertEquals(AdaptiveVerificationState.COMPLETED, completed.state());
    }

    @Test
    void noMatchDoesNotEscalate() {
        BiometricPolicyService policies = mock(BiometricPolicyService.class);
        VideoVerificationEngine engine = mock(VideoVerificationEngine.class);
        BiometricPolicy single = policy(BiometricPolicyMethod.HYBRID_SINGLE_FRAME, "HYBRID_MULTI_FRAME");
        when(policies.createSession(eq("user-1"), isNull(), eq("HYBRID_SINGLE_FRAME")))
                .thenReturn(new BiometricPolicyService.PolicySession("policy-1", "user-1", single, Instant.now().plusSeconds(120)));
        when(engine.verifySingleImage(anyString(), any(), eq("user-1")))
                .thenReturn(new VideoVerificationEngine.SingleImageOutcome("NO_MATCH", 0.31, 0.9, 0.8, 5, List.of("SIMILARITY_BELOW_THRESHOLD")));

        AdaptiveVerificationService service = new AdaptiveVerificationService(policies, engine, mock(HybridMultiFrameVerificationService.class),
                mock(VideoClipDecoder.class), mock(FaceMatchingService.class), mock(BiometricProperties.class), mock(FaceMatcher.class));
        var created = service.create("user-1", null, "HYBRID_SINGLE_FRAME");
        var result = service.verifyImage(created.sessionId(), new byte[]{1});
        assertEquals("NO_MATCH", result.result());
        assertFalse(result.escalated());
        assertEquals(AdaptiveVerificationState.COMPLETED, result.state());
        verify(policies, never()).escalationPolicyForMethod(anyString());
    }

    @Test
    void terminalSessionRejectsReplay() {
        BiometricPolicyService policies = mock(BiometricPolicyService.class);
        VideoVerificationEngine engine = mock(VideoVerificationEngine.class);
        BiometricPolicy single = policy(BiometricPolicyMethod.HYBRID_SINGLE_FRAME, null);
        when(policies.createSession(eq("user-1"), isNull(), eq("HYBRID_SINGLE_FRAME")))
                .thenReturn(new BiometricPolicyService.PolicySession("policy-1", "user-1", single, Instant.now().plusSeconds(120)));
        when(engine.verifySingleImage(anyString(), any(), eq("user-1")))
                .thenReturn(new VideoVerificationEngine.SingleImageOutcome("NO_MATCH", 0.2, 0.9, 0.8, 5, List.of("SIMILARITY_BELOW_THRESHOLD")));
        AdaptiveVerificationService service = new AdaptiveVerificationService(policies, engine, mock(HybridMultiFrameVerificationService.class),
                mock(VideoClipDecoder.class), mock(FaceMatchingService.class), mock(BiometricProperties.class), mock(FaceMatcher.class));
        var created = service.create("user-1", null, "HYBRID_SINGLE_FRAME");
        service.verifyImage(created.sessionId(), new byte[]{1});
        assertThrows(AdaptiveVerificationService.AdaptiveVerificationException.class,
                () -> service.verifyImage(created.sessionId(), new byte[]{1}));
    }

    private BiometricPolicy policy(BiometricPolicyMethod method, String fallback) {
        return new BiometricPolicy("test-" + method.wireValue(), 1, "TEST", method,
                new BiometricPolicy.CaptureRequirements(0, 10, 4, 4, 25_000_000),
                new BiometricPolicy.LivenessRequirements("PASSIVE", true, 0.5), 0.45,
                new BiometricPolicy.RecognitionRequirements("arcface-512", "w600k-r50", 0.65),
                fallback, 120, Instant.now());
    }
}
