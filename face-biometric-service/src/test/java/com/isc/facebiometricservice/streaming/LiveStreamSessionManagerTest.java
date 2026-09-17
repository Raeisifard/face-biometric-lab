package com.isc.facebiometricservice.streaming;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

class LiveStreamSessionManagerTest {

    @Test
    void createsSessionAndRejectsExpiredFrames() {
        LiveStreamSessionManager manager = new LiveStreamSessionManager(Duration.ofMinutes(1));

        VerificationSession session = manager.createSession("customer-001", "LIVE_STREAM");

        assertNotNull(session.sessionId());
        assertEquals("customer-001", session.customerReferenceId());
        assertEquals("LIVE_STREAM", session.expectedCaptureMode());
        assertEquals("CAPTURING", session.processingState());
        assertTrue(manager.isActive(session.sessionId()));

        manager.expireSession(session.sessionId());

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> manager.validateFrame(session.sessionId(), new byte[] {1, 2, 3}));
        assertTrue(ex.getMessage().contains("expired"));
    }

    @Test
    void keepsRecognitionPendingUntilEnoughFramesArrive() {
        LiveStreamSessionManager manager = new LiveStreamSessionManager(Duration.ofMinutes(2));
        VerificationSession session = manager.createSession("customer-002", "LIVE_STREAM");

        var first = manager.recordFrame(session.sessionId(), new byte[] {1, 2, 3, 4});
        assertEquals("GOOD_FRAME", first.feedbackCode());

        var second = manager.recordFrame(session.sessionId(), new byte[] {1, 2, 3, 4});
        assertEquals("LIVENESS_ANALYSIS", second.state());
        assertEquals(2, second.frameCount());
        assertEquals("LIVENESS_PROGRESS", second.feedbackCode());

        manager.markCaptureComplete(session.sessionId());
        VerificationSession updated = manager.getSession(session.sessionId());

        assertEquals("RECOGNITION_PENDING", updated.processingState());
        assertEquals("VERIFICATION_INCONCLUSIVE: RECOGNITION_PENDING", updated.finalResult());
        assertEquals("RECOGNITION_PENDING", manager.getLatestFeedback(session.sessionId()).code());

        manager.recordFrame(session.sessionId(), new byte[] {1, 2, 3, 4});
        manager.recordFrame(session.sessionId(), new byte[] {1, 2, 3, 4});
        manager.recordFrame(session.sessionId(), new byte[] {1, 2, 3, 4});
        manager.markCaptureComplete(session.sessionId());

        assertEquals("VERIFICATION_COMPLETE", manager.getSession(session.sessionId()).processingState());
        assertEquals("VERIFICATION_COMPLETE: RECOGNITION_SUCCESS", manager.getSession(session.sessionId()).finalResult());
    }

    @Test
    void aggregatesLivenessAcrossFrames() {
        LiveStreamSessionManager manager = new LiveStreamSessionManager(frame ->
                new LiveFrameAnalyzer.Analysis("GOOD_FRAME", "Accepted", frame[0] / 10.0, 1));
        VerificationSession session = manager.createSession("customer-003", "LIVE_STREAM");

        manager.recordFrame(session.sessionId(), new byte[] {8});
        manager.recordFrame(session.sessionId(), new byte[] {6});
        manager.markCaptureComplete(session.sessionId());

        assertEquals(0.7, manager.getSession(session.sessionId()).livenessScore(), 1e-9);
    }

    @Test
    void preservesLivenessFailureFromEarlierFrame() {
        LiveStreamSessionManager manager = new LiveStreamSessionManager(new LiveFrameAnalyzer() {
            private int frame;

            @Override
            public Analysis analyze(byte[] payload) {
                frame++;
                return frame == 1
                        ? new Analysis("LIVENESS_FAILED", "Spoof detected", 0.1, 1)
                        : new Analysis("GOOD_FRAME", "Accepted", 0.95, 1);
            }
        });
        VerificationSession session = manager.createSession("customer-004", "LIVE_STREAM");

        manager.recordFrame(session.sessionId(), new byte[] {1});
        manager.recordFrame(session.sessionId(), new byte[] {1});
        manager.markCaptureComplete(session.sessionId());

        assertEquals("NO_MATCH", manager.getSession(session.sessionId()).result());
        assertEquals("VERIFICATION_FAILED: LIVENESS_FAILED", manager.getSession(session.sessionId()).finalResult());
        assertEquals(0.525, manager.getSession(session.sessionId()).livenessScore(), 1e-9);
    }

    @Test
    void staticFramesRemainPendingWithoutTemporalEvidence() {
        LiveStreamSessionManager manager = new LiveStreamSessionManager(frame ->
                new LiveFrameAnalyzer.Analysis("GOOD_FRAME", "Static face", 0.95, 1, 0.0, false));
        VerificationSession session = manager.createSession("customer-005", "LIVE_STREAM");

        for (int i = 0; i < 5; i++) manager.recordFrame(session.sessionId(), new byte[] {1});
        manager.markCaptureComplete(session.sessionId());

        assertEquals("INCONCLUSIVE", manager.getSession(session.sessionId()).result());
        assertEquals("VERIFICATION_INCONCLUSIVE: TEMPORAL_EVIDENCE_PENDING", manager.getSession(session.sessionId()).finalResult());
    }

    @Test
    void movingFramesCanCompleteSequenceLiveness() {
        LiveStreamSessionManager manager = new LiveStreamSessionManager(frame ->
                new LiveFrameAnalyzer.Analysis("GOOD_FRAME", "Temporal evidence accepted", 0.95, 1, 0.08, true));
        VerificationSession session = manager.createSession("customer-006", "LIVE_STREAM");

        for (int i = 0; i < 5; i++) manager.recordFrame(session.sessionId(), new byte[] {1});
        manager.markCaptureComplete(session.sessionId());

        assertEquals("MATCH", manager.getSession(session.sessionId()).result());
        assertEquals("VERIFICATION_COMPLETE: RECOGNITION_SUCCESS", manager.getSession(session.sessionId()).finalResult());
    }
}
