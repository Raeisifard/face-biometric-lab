package com.isc.facebiometricservice.streaming;

public interface LiveFrameAnalyzer {
    Analysis analyze(byte[] frame);

    record Analysis(String feedbackCode, String message) {
    }
}