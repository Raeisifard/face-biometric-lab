package com.isc.faceclientsimulator.service;

import com.isc.faceclientsimulator.ai.*;
import com.isc.faceclientsimulator.domain.*;
import com.isc.faceclientsimulator.liveness.TemporalLivenessEngine;
import org.opencv.core.Mat;
import org.springframework.stereotype.Service;

@Service
public class BiometricFrameProcessor {
    private final YuNetFaceDetector detector;
    private final MiniFasNetV2LivenessModel liveness;
    private final ArcFaceOnnxEmbeddingModel embedding;
    private final TemporalLivenessEngine temporal;

    public BiometricFrameProcessor(YuNetFaceDetector detector,
                                    MiniFasNetV2LivenessModel liveness,
                                    ArcFaceOnnxEmbeddingModel embedding,
                                    TemporalLivenessEngine temporal) {
        this.detector=detector; this.liveness=liveness; this.embedding=embedding; this.temporal=temporal;
    }

    public FrameAnalysis analyze(String sessionId, byte[] bytes) {
        long started=System.nanoTime();
        Mat image=OpenCvImageCodec.decode(bytes);
        try {
            FaceDetectionResult detection=detector.detect(image);
            if(!detection.faceDetected()) {
                TemporalLivenessEngine.Result r=temporal.accept(sessionId,detection,0);
                return new FrameAnalysis(sessionId,detection,toDto(r,0,System.nanoTime()-started),false);
            }
            double liveScore=liveness.liveScore(image,detection.box());
            TemporalLivenessEngine.Result r=temporal.accept(sessionId,detection,liveScore);
            return new FrameAnalysis(sessionId,detection,toDto(r,liveScore,System.nanoTime()-started),r.live());
        } finally { image.release(); }
    }

    public void resetSession(String sessionId) { temporal.reset(sessionId); }

    public FaceEmbedding generateEmbedding(byte[] bytes) {
        Mat image=OpenCvImageCodec.decode(bytes);
        try {
            FaceDetectionResult detection=detector.detect(image);
            if(!detection.faceDetected()) throw new IllegalArgumentException("No face detected");
            return embedding.generate(image,detection);
        } finally { image.release(); }
    }

    private LivenessFrameResult toDto(TemporalLivenessEngine.Result r,double liveScore,long nanos) {
        return new LivenessFrameResult(r.status(),r.live(),liveScore,r.temporalMotion(),r.acceptedFrames(),r.requiredFrames(),r.instruction(),nanos/1_000_000);
    }
}
