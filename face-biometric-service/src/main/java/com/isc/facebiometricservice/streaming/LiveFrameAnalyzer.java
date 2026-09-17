package com.isc.facebiometricservice.streaming;

public interface LiveFrameAnalyzer {
    Analysis analyze(byte[] frame);

    default Analysis analyze(String sessionId, byte[] frame) {
        return analyze(frame);
    }

    record Analysis(String feedbackCode, String message, Double livenessScore, int detectedFaces,
                    Double temporalMotion, boolean temporalReady) {
        public Analysis(String feedbackCode, String message) {
            this(feedbackCode, message, null, 0, null, false);
        }

        public Analysis(String feedbackCode, String message, Double livenessScore, int detectedFaces) {
            this(feedbackCode, message, livenessScore, detectedFaces, null, false);
        }
    }
}