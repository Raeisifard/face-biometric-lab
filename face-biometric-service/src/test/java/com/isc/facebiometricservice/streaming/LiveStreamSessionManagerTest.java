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
    void recordsFeedbackAndCompletesCapture() {
        LiveStreamSessionManager manager = new LiveStreamSessionManager(Duration.ofMinutes(2));
        VerificationSession session = manager.createSession("customer-002", "LIVE_STREAM");

        var first = manager.recordFrame(session.sessionId(), new byte[] {1, 2, 3, 4});
        assertEquals("GOOD_FRAME", first.feedbackCode());

        var second = manager.recordFrame(session.sessionId(), new byte[] {1, 2, 3, 4});
        assertEquals("CAPTURE_CONTINUE", second.state());

        manager.markCaptureComplete(session.sessionId());
        VerificationSession updated = manager.getSession(session.sessionId());

        assertEquals("CAPTURE_COMPLETE", updated.processingState());
        assertEquals("CAPTURE_COMPLETE", manager.getLatestFeedback(session.sessionId()).code());
    }
}
