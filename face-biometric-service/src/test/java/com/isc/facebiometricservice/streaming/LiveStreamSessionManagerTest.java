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
}
